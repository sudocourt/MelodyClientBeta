package me.earth.earthhack.impl.modules.render.esp;

import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.render.esp.mode.EspMode;
import me.earth.earthhack.impl.util.minecraft.PhaseUtil;
import me.earth.earthhack.impl.util.render.Render3DUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ListenerRender extends ModuleListener<ESP, Render3DEvent> {

    public ListenerRender(ESP module) {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        var matrices = event.getStack();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();

        matrices.push();
        // Сдвигаем мир относительно камеры (матрица из события пустая, поэтому это обязательно)
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

        if (module.storage.getValue()) {
            drawBlockEntities(event, Managers.ENTITIES.getBlockEntities());
        }

        if (module.players.getValue() || module.monsters.getValue() || module.animals.getValue() || module.vehicles.getValue() || module.misc.getValue() || module.items.getValue()) {
            drawEntities(event, Managers.ENTITIES.getEntities(), tickDelta);
        }

        matrices.pop();
    }

    private void drawEntities(Render3DEvent event, List<Entity> playerEntities, float tickDelta) {
        for (Entity entity : playerEntities) {
            if (entity.equals(mc.player) || entity.distanceTo(mc.player) > module.range.getValue() || entity instanceof EndCrystalEntity) {
                continue;
            }
            if (entity instanceof FireworkRocketEntity rocket && rocket.getOwner() != null && rocket.getOwner().equals(mc.player)) {
                continue;
            }

            // Идеальная интерполяция как в Meteor Client
            Vec3d lerpedPos = getLerpedPos(entity, tickDelta);
            Box bb = entity.getBoundingBox().offset(lerpedPos.subtract(entity.getPos()));

            Color color = null;
            if (entity instanceof PlayerEntity player) {
                if (module.players.getValue()) {
                    color = module.playersColor.getValue();
                    if (entity.isInvisible()) color = module.invisibleColor.getValue();
                    if (PhaseUtil.isPhasing(player, module.pushMode.getValue())) color = module.phaseColor.getValue();
                    if (entity.equals(Managers.TARGET.getAutoCrystal()) || entity.equals(Managers.TARGET.getAutoTrap()) || entity.equals(Managers.TARGET.getKillAura())) color = module.targetColor.getValue();
                    if (Managers.FRIENDS.contains(player)) color = module.friendColor.getValue();
                }
            } else if (entity instanceof ItemEntity item) {
                if (module.items.getValue()) {
                    Vec3d itemLerped = getLerpedPos(item, tickDelta);
                    bb = new Box(-0.25, 0, -0.25, 0.25, 0.50, 0.25).offset(itemLerped);
                    color = module.itemsColor.getValue();
                }
            } else if (entity instanceof VehicleEntity) {
                if (module.vehicles.getValue()) color = module.vehiclesColor.getValue();
            } else if (entity instanceof AnimalEntity) {
                if (module.animals.getValue()) color = module.animalsColor.getValue();
            } else if (entity instanceof MobEntity) {
                if (module.monsters.getValue()) color = module.monstersColor.getValue();
            } else if (module.misc.getValue()) {
                color = module.miscColor.getValue();
            }

            if (color == null) continue;
            renderBox(event, bb, color);
        }
    }

    // Вспомогательный метод для интерполяции
    private Vec3d getLerpedPos(Entity e, float partialTicks) {
        if (e.isRemoved()) return e.getPos();
        double x = MathHelper.lerp(partialTicks, e.lastRenderX, e.getX());
        double y = MathHelper.lerp(partialTicks, e.lastRenderY, e.getY());
        double z = MathHelper.lerp(partialTicks, e.lastRenderZ, e.getZ());
        return new Vec3d(x, y, z);
    }

    private void drawBlockEntities(Render3DEvent event, List<BlockEntity> blockEntities) {
        List<BlockPos> skippedDoubleChests = new ArrayList<>();

        for (BlockEntity blockEntity : blockEntities) {
            if (!blockEntity.getPos().isWithinDistance(mc.player.getPos(), module.storageRange.getValue()) || skippedDoubleChests.contains(blockEntity.getPos())) {
                continue;
            }

            Box bb = blockEntity.getCachedState().getCollisionShape(blockEntity.getWorld(), blockEntity.getPos()).getBoundingBox();
            Box newBB = bb.offset(blockEntity.getPos());

            if (blockEntity instanceof ChestBlockEntity chest) {
                BlockState blockState = blockEntity.getCachedState();
                ChestType chestType = blockState.contains((Property<?>) ChestBlock.CHEST_TYPE) ? blockState.get(ChestBlock.CHEST_TYPE) : ChestType.SINGLE;
                if (chestType != ChestType.SINGLE) {
                    Direction direction = blockState.get(ChestBlock.FACING);
                    BlockPos otherPos = chestType == ChestType.LEFT ? chest.getPos().offset(direction.rotateYClockwise()) : chest.getPos().offset(direction.rotateYCounterclockwise());
                    BlockEntity otherBlockEntity = chest.getWorld().getBlockEntity(otherPos);
                    if (otherBlockEntity instanceof ChestBlockEntity otherChest) {
                        Box otherBB = otherChest.getCachedState().getCollisionShape(otherChest.getWorld(), otherChest.getPos()).getBoundingBox();
                        Box otherNewBB = otherBB.offset(otherChest.getPos());
                        newBB = newBB.union(otherNewBB);
                        skippedDoubleChests.add(otherChest.getPos());
                    }
                }
            }

            Color fillColor = module.colorTileEntityInside(blockEntity);
            Color outlineColor = module.colorTileEntity(blockEntity);
            renderBox(event, newBB, module.mode.getValue() == EspMode.Outline ? outlineColor : fillColor);
        }
    }

    private void renderBox(Render3DEvent event, Box box, Color color) {
        switch (module.mode.getValue()) {
            case Outline -> Render3DUtil.drawOutline(event.getStack(), box, color);
            case Box -> Render3DUtil.drawBox(event.getStack(), box, color);
            case BoxOutline -> {
                Render3DUtil.drawBox(event.getStack(), box, color.darker());
                Render3DUtil.drawOutline(event.getStack(), box, color);
            }
        }
    }
}