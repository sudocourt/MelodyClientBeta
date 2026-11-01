package me.earth.earthhack.impl.modules.player.fakeplayer;

import com.mojang.authlib.GameProfile;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.api.setting.settings.StringSetting;
import me.earth.earthhack.impl.modules.player.fakeplayer.util.EntityPlayerPop;
import me.earth.earthhack.impl.modules.player.fakeplayer.util.Position;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.helpers.disabling.DisablingModule;
import me.earth.earthhack.impl.util.math.StopWatch;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static me.earth.earthhack.api.util.interfaces.Globals.mc;

public class FakePlayer extends DisablingModule
{
    protected final Setting<Boolean> playRecording = register(new BooleanSetting("Play-Recording", false));
    protected final Setting<Boolean> record = register(new BooleanSetting("Record", false));
    protected final Setting<Boolean> loop = register(new BooleanSetting("Loop", false));
    protected final Setting<Boolean> gapple = register(new BooleanSetting("Gapple", true));
    protected final Setting<Integer> gappleDelay = register(new NumberSetting<>("Gapple-Delay", 1600, 1500, 2000));
    protected final Setting<Boolean> damage = register(new BooleanSetting("Damage", true));

    public final Setting<String> name = register(new StringSetting("PlayerName", "FakePlayer"));
    // ИЗМЕНЕНО НА RANDOM
    public final Setting<String> skinSelection = register(new StringSetting("Skin", "Random"));

    protected final List<Position> positions = new ArrayList<>();
    protected final StopWatch timer = new StopWatch();
    protected OtherClientPlayerEntity fakePlayer;
    protected int index;

    public final Map<String, Identifier> allSkins = new LinkedHashMap<>();
    private final Map<String, SkinTextures.Model> skinModels = new HashMap<>();

    private double savedX, savedY, savedZ;
    private float savedYaw, savedPitch;
    private boolean hasSavedPos = false;

    public FakePlayer()
    {
        super("FakePlayer", Category.Player);
        this.listeners.add(new ListenerMotion(this));
        this.listeners.add(new ListenerWorldClient(this));
        this.listeners.add(new ListenerAttack(this));
        this.listeners.add(new ListenerExplosion(this));
        this.setData(new SimpleData(this, "Spawns in a FakePlayer for testing purposes."));

        // УБРАЛИ loadAllSkins() ОТСЮДА

        this.name.addObserver(event -> {
            if (this.isEnabled() && fakePlayer != null) {
                savePositionAndRespawn(event.getValue(), skinSelection.getValue());
            }
        });

        this.skinSelection.addObserver(event -> {
            if (this.isEnabled() && fakePlayer != null) {
                savePositionAndRespawn(name.getValue(), event.getValue());
            }
        });
    }

    private void savePositionAndRespawn(String newName, String newSkin) {
        savedX = fakePlayer.getX();
        savedY = fakePlayer.getY();
        savedZ = fakePlayer.getZ();
        savedYaw = fakePlayer.getYaw();
        savedPitch = fakePlayer.getPitch();
        hasSavedPos = true;

        OtherClientPlayerEntity oldPlayer = fakePlayer;
        fakePlayer = null;

        mc.execute(() -> {
            if (oldPlayer != null) oldPlayer.discard();
            spawnFakePlayer(newName, newSkin);
        });
    }

    @Override
    public String getDisplayInfo() {
        return record.getValue() ? "Recording" : playRecording.getValue() ? "Playing" : null;
    }

    @Override
    protected void onEnable()
    {
        hasSavedPos = false;
        // На всякий случай обновляем при включении (если ты докинул файлы на лету)
        loadAllSkins();
        spawnFakePlayer(name.getValue(), skinSelection.getValue());
    }

    @Override
    protected void onDisable()
    {
        if (fakePlayer != null) {
            fakePlayer.discard();
            fakePlayer = null;
        }
        playRecording.setValue(false);
        record.setValue(false);
        hasSavedPos = false;
    }

    private void spawnFakePlayer(String playerName, String selection)
    {
        if (playerName == null || playerName.isEmpty()) playerName = "FakePlayer";

        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(uuid, playerName);

        index = 0;

        String chosenSkin = null;

        // ЛОГИКА ВЫБОРА СКИНА
        if (!selection.equalsIgnoreCase("Random") && allSkins.containsKey(selection)) {
            chosenSkin = selection;
        } else {
            if (allSkins.containsKey(playerName)) {
                chosenSkin = playerName;
            } else if (!allSkins.isEmpty()) {
                List<String> keys = new ArrayList<>(allSkins.keySet());
                chosenSkin = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
            }
        }

        if (chosenSkin != null) {
            EntityPlayerPop.CUSTOM_SKIN = allSkins.get(chosenSkin);
            EntityPlayerPop.SKIN_MODEL = skinModels.getOrDefault(chosenSkin, SkinTextures.Model.WIDE);
        } else {
            EntityPlayerPop.CUSTOM_SKIN = null;
            EntityPlayerPop.SKIN_MODEL = SkinTextures.Model.WIDE;
        }

        EntityPlayerPop newPlayer = new EntityPlayerPop(mc.world, profile);
        newPlayer.refreshPositionAndAngles(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch());
        newPlayer.setHeadYaw(mc.player.getYaw());

        mc.world.addEntity(newPlayer);
        fakePlayer = newPlayer;

        if (fakePlayer != null) {
            fakePlayer.getAbilities().invulnerable = !damage.getValue();
            if (hasSavedPos) {
                fakePlayer.refreshPositionAndAngles(savedX, savedY, savedZ, savedYaw, savedPitch);
                fakePlayer.setHeadYaw(savedYaw);
            }
        }
    }

    public void loadAllSkins()
    {
        // ЗАЩИТА ОТ КРАША ПРИ СТАРТЕ ИГРЫ
        if (mc == null || mc.runDirectory == null) return;

        allSkins.clear();
        skinModels.clear();

        File baseDir = new File(mc.runDirectory, "earthhack/fakeplayer");
        File wideDir = new File(baseDir, "wide");
        File slimDir = new File(baseDir, "slim");

        if (!wideDir.exists()) wideDir.mkdirs();
        if (!slimDir.exists()) slimDir.mkdirs();

        loadSkinsFromFolder(wideDir, SkinTextures.Model.WIDE);
        loadSkinsFromFolder(slimDir, SkinTextures.Model.SLIM);
    }

    private void loadSkinsFromFolder(File folder, SkinTextures.Model model)
    {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null) return;

        for (File file : files) {
            try (InputStream is = new FileInputStream(file)) {
                NativeImage image = NativeImage.read(is);
                String cleanName = file.getName().substring(0, file.getName().length() - 4);
                String safeName = cleanName.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
                Identifier skinId = Identifier.of("earthhack", "fp_" + safeName + "_" + file.lastModified());

                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                texture.upload();
                mc.getTextureManager().registerTexture(skinId, texture);

                if (allSkins.containsKey(cleanName)) {
                    cleanName = cleanName + "_" + folder.getName();
                }

                allSkins.put(cleanName, skinId);
                skinModels.put(cleanName, model);
            } catch (Exception e) {
                System.out.println("[FakePlayer] Failed to load skin: " + file.getName());
            }
        }
    }
}