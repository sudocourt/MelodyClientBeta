package me.earth.earthhack.impl.gui.click;

import com.mojang.blaze3d.systems.RenderSystem;
import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.cache.SettingCache;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.gui.click.component.Component;
import me.earth.earthhack.impl.gui.click.component.impl.ColorComponent;
import me.earth.earthhack.impl.gui.click.component.impl.KeybindComponent;
import me.earth.earthhack.impl.gui.click.component.impl.ModuleComponent;
import me.earth.earthhack.impl.gui.click.component.impl.StringComponent;
import me.earth.earthhack.impl.gui.click.frame.Frame;
import me.earth.earthhack.impl.gui.click.frame.impl.CategoryFrame;
import me.earth.earthhack.impl.gui.click.frame.impl.DescriptionFrame;
import me.earth.earthhack.impl.gui.click.frame.impl.ModulesFrame;
import me.earth.earthhack.impl.gui.click.frame.impl.SearchFrame;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.managers.client.ModuleManager;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.clickgui.ClickGui;
import me.earth.earthhack.impl.modules.client.commands.Commands;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.system.NonnullDefault;

import java.awt.*;
import java.util.ArrayList;

import static me.earth.earthhack.api.util.interfaces.Globals.mc;

public class Click extends Screen {
    public static final ModuleCache<ClickGui> CLICK_GUI = Caches.getModule(ClickGui.class);

    private static final SettingCache<Boolean, BooleanSetting, Commands> BACK =
            Caches.getSetting(Commands.class, BooleanSetting.class, "BackgroundGui", false);
    private static final Identifier BLACK_PNG =
            Identifier.of("earthhack:textures/gui/black.png");
    private final ArrayList<Frame> frames = new ArrayList<>();
    private Category[] categories = Category.values();
    private final ModuleManager moduleManager;
    private boolean oldVal = false;
    private boolean attached = false;
    private boolean addDescriptionFrame = true;
    private boolean pingBypass;

    public final Screen screen;

    public static DescriptionFrame descriptionFrame =
            new DescriptionFrame(0, 0, 200, 18);

    public Click(Screen screen) {
        super(Text.of("ClickGui"));
        this.moduleManager = Managers.MODULES;
        this.screen = screen;
    }

    public Click(Screen screen, ModuleManager moduleManager) {
        super(Text.of("ClickGui"));
        this.moduleManager = moduleManager;
        this.screen = screen;
    }

    public void init() {
        if (!attached) {
            CLICK_GUI.get().descriptionWidth.addObserver(e -> descriptionFrame.setWidth(e.getValue()));
            attached = true;
        }

        getFrames().clear();
        int x = CLICK_GUI.get().catEars.getValue() ? 14 : 2;
        int y = CLICK_GUI.get().catEars.getValue() ? 14 : 2;
        for (Category moduleCategory : categories) {
            getFrames().add(new CategoryFrame(moduleCategory, moduleManager, x, y, 110, 16));
            if (x + 220 >= MinecraftClient.getInstance().getWindow().getScaledWidth()) {
                x = CLICK_GUI.get().catEars.getValue() ? 14 * Math.round(CLICK_GUI.get().guiScale.getValue()) : 2;
                y += CLICK_GUI.get().catEars.getValue() ? (int) (32 * CLICK_GUI.get().guiScale.getValue()) : 20;
            } else
                x += CLICK_GUI.get().catEars.getValue() ? (int) (132 * CLICK_GUI.get().guiScale.getValue()) : 112;
        }

        if (addDescriptionFrame) {
            descriptionFrame = new DescriptionFrame(CLICK_GUI.get().descPosX.getValue(), CLICK_GUI.get().descPosY.getValue(), CLICK_GUI.get().descriptionWidth.getValue(), 16);
            getFrames().add(descriptionFrame);
        }

        if (pingBypass) {
            DescriptionFrame hint = new DescriptionFrame("Info", x, y + 100, CLICK_GUI.get().descriptionWidth.getValue(), 16);
            hint.setDescription("You are editing the modules running on the PingBypass server, not the ones which run here on your client.");
            getFrames().add(hint);

            ModulesFrame pbFrame = new ModulesFrame("PingBypass", x, y + 200, 110, 16);
            getFrames().add(pbFrame);
        }

        if (CLICK_GUI.get().search.getValue() != ClickGui.SearchStyle.None) {
            SearchFrame searchFrame = new SearchFrame();
            getFrames().add(searchFrame);
            searchFrame.clearInput();
        }

        getFrames().forEach(Frame::init);
        oldVal = CLICK_GUI.get().catEars.getValue();
    }

    @Override
    @NonnullDefault
    public void resize(MinecraftClient mcIn, int w, int h) {
        super.resize(mcIn, w, h);
        init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // ОБНОВЛЯЕМ ВРЕМЯ КАДРА ДЛЯ ПЛАВНЫХ АНИМАЦИЙ (lerp)
        Render2DUtil.updateFrameClock();

        // --- СОВРЕМЕННЫЙ ФОН (BLUR & DIM) ---
        if (mc.world != null) {
            // Мягкое затемнение с помощью градиента (винетка)
            context.fillGradient(0, 0, this.width, this.height, 0x60000000, 0x90000000);

            if (CLICK_GUI.get().blur.getValue() == ClickGui.BlurStyle.Directional) {
                Render2DUtil.drawBlurryRect(
                        context.getMatrices(),
                        0, 0, this.width, this.height,
                        CLICK_GUI.get().blurAmount.getValue(),
                        CLICK_GUI.get().blurSize.getValue()
                );
            }
        }

        super.render(context, mouseX, mouseY, delta);

        // Стандартный фон для главного меню (когда мы не в мире)
        if (mc.world == null) {
            if (BACK.getValue()) {
                this.renderBackground(context, mouseX, mouseY, delta);
            } else {
                // В 1.21.1 renderBackground и так заливает всё черным,
                // поэтому кастомный BufferBuilder с LINE_STRIP можно смело выкинуть.
                context.fill(0, 0, this.width, this.height, 0xFF000000);
            }
        }

        if (oldVal != CLICK_GUI.get().catEars.getValue()) {
            init();
            oldVal = CLICK_GUI.get().catEars.getValue();
        }

        ModuleComponent.context = context;
        getFrames().forEach(frame -> frame.drawScreen(context, mouseX, mouseY, delta));
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        getFrames().forEach(frame -> frame.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        getFrames().forEach(frame -> frame.keyPressed(keyCode));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        getFrames().forEach(frame -> frame.mouseScrolled(mouseX, mouseY, verticalAmount));
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        getFrames().forEach(frame -> frame.mouseClicked(mouseX, mouseY, mouseButton));
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        getFrames().forEach(frame -> frame.mouseReleased(mouseX, mouseY, mouseButton));
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        super.close();
        getFrames().forEach(frame -> {
            for (Component comp : frame.getComponents()) {
                if (comp instanceof ModuleComponent moduleComponent) {
                    for (Component component : moduleComponent.getComponents()) {
                        if (component instanceof KeybindComponent keybindComponent) {
                            keybindComponent.setBinding(false);
                        }
                        if (component instanceof StringComponent stringComponent) {
                            stringComponent.setListening(false);
                        }
                    }
                }
            }
        });
        CLICK_GUI.disable();
    }

    public void onGuiOpened() {
        getFrames().forEach(frame -> {
            for (Component comp : frame.getComponents()) {
                if (comp instanceof ModuleComponent moduleComponent) {
                    for (Component component : moduleComponent.getComponents()) {
                        if (component instanceof ColorComponent colorComponent) {
                            float[] hsb = Color.RGBtoHSB(colorComponent.getColorSetting().getRed(), colorComponent.getColorSetting().getGreen(), colorComponent.getColorSetting().getBlue(), null);
                            colorComponent.setHue(hsb[0]);
                            colorComponent.setSaturation(hsb[1]);
                            colorComponent.setBrightness(hsb[2]);
                            colorComponent.setAlpha(colorComponent.getColorSetting().getAlpha() / 255.f);
                        }
                    }
                }
            }
        });
    }

    public ArrayList<Frame> getFrames() {
        return frames;
    }

    public void setPingBypass(boolean pingBypass) {
        this.pingBypass = pingBypass;
    }

    public void setAddDescriptionFrame(boolean addDescriptionFrame) {
        this.addDescriptionFrame = addDescriptionFrame;
    }

    public void setCategories(Category[] categories) {
        this.categories = categories;
    }
}