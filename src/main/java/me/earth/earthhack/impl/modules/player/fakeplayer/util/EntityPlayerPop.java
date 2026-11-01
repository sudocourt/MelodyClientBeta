package me.earth.earthhack.impl.modules.player.fakeplayer.util;

import com.mojang.authlib.GameProfile;
import me.earth.earthhack.impl.util.minecraft.ICachedDamage;
import me.earth.earthhack.impl.util.network.NetworkUtil;
import me.earth.earthhack.impl.util.thread.EnchantmentUtil;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;

import java.util.UUID;

import static me.earth.earthhack.api.util.interfaces.Globals.mc;

public class EntityPlayerPop extends OtherClientPlayerEntity implements ICachedDamage
{
    public static Identifier CUSTOM_SKIN = null;
    // ВОТ ОНА, ПЕРЕМЕННАЯ МОДЕЛИ. Без неё SLIM-скины ломаются
    public static SkinTextures.Model SKIN_MODEL = SkinTextures.Model.WIDE;

    @SuppressWarnings("unused")
    public EntityPlayerPop(ClientWorld worldIn)
    {
        super(worldIn, new GameProfile(UUID.randomUUID(), "FakePlayer"));
    }

    public EntityPlayerPop(ClientWorld worldIn, GameProfile gameProfileIn)
    {
        super(worldIn, gameProfileIn);
    }

    @Override
    public SkinTextures getSkinTextures()
    {
        if (CUSTOM_SKIN != null)
        {
            // Используем SKIN_MODEL (WIDE или SLIM)
            return new SkinTextures(CUSTOM_SKIN, null, null, null, SKIN_MODEL, false);
        }
        return super.getSkinTextures();
    }

    @Override
    public boolean isPartVisible(PlayerModelPart modelPart)
    {
        return true;
    }

    @SuppressWarnings("NullableProblems")
    public ItemStack getItemStackFromSlot(EquipmentSlot slotIn)
    {
        if (slotIn == EquipmentSlot.OFFHAND)
        {
            ItemStack stack = new ItemStack(Items.TOTEM_OF_UNDYING);
            stack.setCount(1);
            return stack;
        }
        return super.getInventory().getStack(slotIn.getEntitySlotId());
    }

    @Override
    public void setHealth(float health)
    {
        if (health <= 0.0f)
        {
            pop();
            return;
        }
        super.setHealth(health);
    }

    public void setDead() { }

    public void pop()
    {
        NetworkUtil.receive(new EntityStatusS2CPacket(this, (byte) 35));
        super.setHealth(1.0f);
        this.setAbsorptionAmount(8.0f);
        this.clearStatusEffects();
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
    }

    @Override
    public int getArmorValue() { return mc.player.getArmor(); }

    @Override
    public float getArmorToughness() {
        return (float) mc.player.getAttributes().getValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
    }

    @Override
    public int getExplosionModifier(DamageSource source) {
        return EnchantmentUtil.getEnchantmentModifierDamage(this.getArmorItems(), source);
    }
}