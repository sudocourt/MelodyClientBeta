package me.earth.earthhack.impl.gui.click.component.impl;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.data.ModuleData;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.*;
import me.earth.earthhack.impl.gui.chat.factory.ComponentFactory;
import me.earth.earthhack.impl.gui.click.Click;
import me.earth.earthhack.impl.gui.click.component.Component;
import me.earth.earthhack.impl.gui.click.component.SettingComponent;
import me.earth.earthhack.impl.gui.visibility.Visibilities;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.client.clickgui.ClickGui;
import me.earth.earthhack.impl.modules.client.configs.ConfigHelperModule;
import me.earth.earthhack.impl.modules.player.fakeplayer.FakePlayer;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import me.earth.earthhack.impl.util.text.TextColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ModuleComponent extends Component {
    private final Module module;
    private final float height;
    private final ArrayList<Component> components = new ArrayList<>();

    public static DrawContext context;

    private float enabledProgress = 0f;
    private float hoverProgress = 0f;
    private long lastFrameMs = System.currentTimeMillis();

    public ModuleComponent(Module module, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(module.getName(), posX, posY, offsetX, offsetY, width, height);
        this.height = height;
        this.module = module;
        this.enabledProgress = module.isEnabled() ? 1f : 0f;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init() {
        getComponents().clear();
        float offY = getHeight();
        ModuleData<?> data = getModule().getData();
        if (data != null) {
            this.setDescription(data::getDescription);
        }

        if (!getModule().getSettings().isEmpty()) {
            for (Setting<?> setting : getModule().getSettings()) {
                if (!setting.getVisibility()) continue;
                float before = offY;

                // --- ОСОБАЯ ЛОГИКА ДЛЯ ВЫБОРА СКИНА ---
                if (setting.getName().equalsIgnoreCase("Skin") && getModule() instanceof FakePlayer fakePlayer) {
                    getComponents().add(new SkinComponent((StringSetting) setting, fakePlayer, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                // ---------------------------------------
                else if (setting instanceof BooleanSetting && !setting.getName().equalsIgnoreCase("enabled")) {
                    getComponents().add(new BooleanComponent((BooleanSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                else if (setting instanceof BindSetting) {
                    getComponents().add(new KeybindComponent((BindSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                else if (setting instanceof NumberSetting) {
                    getComponents().add(new NumberComponent((NumberSetting<Number>) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                else if (setting instanceof EnumSetting) {
                    getComponents().add(new EnumComponent<>((EnumSetting<?>) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                else if (setting instanceof ColorSetting) {
                    getComponents().add(new ColorComponent((ColorSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                else if (setting instanceof StringSetting) {
                    getComponents().add(new StringComponent((StringSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }
                else if (setting instanceof ListSetting) {
                    getComponents().add(new ListComponent<>((ListSetting<?>) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 14));
                    offY += 14;
                }

                if (data != null && before != offY) {
                    Supplier<String> supplier = () -> {
                        String desc = data.settingDescriptions().get(setting);
                        if (desc == null) {
                            desc = "A Setting (" + setting.getInitial().getClass().getSimpleName() + ").";
                        }
                        if (Click.CLICK_GUI.get().descNameValue.getValue()) {
                            desc = ComponentFactory.create(setting).getText() + "\n\n" + TextColor.WHITE + desc;
                        }
                        return desc;
                    };
                    getComponents().get(getComponents().size() - 1).setDescription(supplier);
                }
            }
        }
        getComponents().forEach(Component::init);
    }

    @Override
    public void moved(float posX, float posY) {
        super.moved(posX, posY);
        getComponents().forEach(component -> component.moved(getFinishedX(), getFinishedY()));
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(context, mouseX, mouseY, partialTicks);
        ModuleComponent.context = context;

        if (!module.searchVisibility) {
            setHeight(0);
            if (isExtended()) setExtended(false);
            return;
        }
        setHeight(height);

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastFrameMs) / 1000f, 0.1f);
        lastFrameMs = now;

        final boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());
        hoverProgress = MathHelper.lerp(Math.min(dt * 20f, 1f), hoverProgress, hovered ? 1f : 0f);
        enabledProgress = MathHelper.lerp(Math.min(dt * 12f, 1f), enabledProgress, getModule().isEnabled() ? 1f : 0f);

        ClickGui gui = getClickGui().get();
        boolean isEnabled = getModule().isEnabled();
        boolean isExp = isExtended();

        int toxicLime = 0xFF39FF14;
        int darkLime = 0xFF1A5C0A;
        int darkerPink = 0xFF3A003A;

        int headerBg = 0;
        int settingsBg = 0;

        int textColor = gui.getOffModule().brighter().getRGB();
        if (isEnabled) {
            textColor = toxicLime;
        }

        if (isExp) {
            if (isEnabled) {
                headerBg = darkLime;
                settingsBg = darkLime;
            } else {
                headerBg = darkerPink;
                settingsBg = darkerPink;
            }
        } else if (hoverProgress > 0.01f) {
            headerBg = Render2DUtil.withAlpha(gui.getModuleHover().getRGB(), (int)(hoverProgress * 0xB0));
        }

        // 1. Фон настроек
        if (isExp) {
            Render2DUtil.drawRect(context.getMatrices(),
                    getFinishedX(), getFinishedY() + getHeight(),
                    getFinishedX() + getWidth(), getFinishedY() + getHeight() + getComponentsSize() + 3,
                    settingsBg);
        }

        // 2. Фон заголовка
        if (headerBg != 0) {
            Render2DUtil.drawRect(context.getMatrices(),
                    getFinishedX(), getFinishedY(),
                    getFinishedX() + getWidth(), getFinishedY() + getHeight(),
                    headerBg);
        }

        // === ЖИРНАЯ ТОЧКА СЛЕВА ===
        float dotX = getFinishedX() + 5;
        float dotY = getFinishedY() + getHeight() / 2f - 1.5f;
        Render2DUtil.drawRect(context.getMatrices(), dotX, dotY, dotX + 3, dotY + 3, textColor);

        // 3. Текст
        String label = module instanceof ConfigHelperModule && ((ConfigHelperModule) module).isDeleted()
                ? net.minecraft.util.Formatting.RED + getLabel() : getLabel();
        float textY = getFinishedY() + getHeight() / 2f - (Managers.TEXT.getStringHeightI() >> 1);

        Managers.TEXT.drawStringWithShadow(context, label, getFinishedX() + 11, textY, textColor);

        if (!getComponents().isEmpty()) {
            String arrow = isExtended() ? gui.close.getValue() : gui.open.getValue();
            Managers.TEXT.drawStringWithShadow(context, arrow, getFinishedX() + getWidth() - 4 - Managers.TEXT.getStringWidth(arrow), textY, textColor);
        }

        if (gui.showBind.getValue() && !getModule().getBind().toString().equalsIgnoreCase("none")) {
            String bind = getModule().getBind().toString().toLowerCase().replace("none", "-");
            bind = "[" + (bind.length() > 3 ? bind.substring(0, 3) : String.valueOf(bind.charAt(0)).toUpperCase() + bind.substring(1)) + "]";
            String arrow = isExtended() ? gui.close.getValue() : gui.open.getValue();
            float offset = getFinishedX() + getWidth() - Managers.TEXT.getStringWidth(arrow) * 2;
            Managers.TEXT.drawStringScaled(context, bind, offset - (Managers.TEXT.getStringWidth(bind) >> 1),
                    getFinishedY() + getHeight() / 1.5f - (Managers.TEXT.getStringHeightI() >> 1), textColor, true, 0.5f);
        }

        // 4. Рисуем настройки
        if (isExtended()) {
            for (Component c : getComponents()) {
                if (c instanceof SettingComponent<?, ?> sc && Visibilities.VISIBILITY_MANAGER.isVisible(sc.getSetting())) {
                    c.drawScreen(context, mouseX, mouseY, partialTicks);
                }
            }
        }

        updatePositions();
    }

    @Override
    public void charTyped(char character, int keyCode) {
        super.charTyped(character, keyCode);
        if (isExtended()) {
            for (Component component : getComponents()) {
                if (component instanceof SettingComponent && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                    component.charTyped(character, keyCode);
                }
            }
        }
    }

    @Override
    public void keyPressed(int keyCode) {
        super.keyPressed(keyCode);
        if (isExtended()) {
            for (Component component : getComponents()) {
                if (component instanceof SettingComponent && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                    component.keyPressed(keyCode);
                }
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());
        if (hovered) {
            switch (mouseButton) {
                case 0 -> getModule().toggle();
                case 1 -> { if (!getComponents().isEmpty()) setExtended(!isExtended()); }
                default -> {}
            }
        }
        if (isExtended()) {
            for (Component component : getComponents()) {
                if (component instanceof SettingComponent && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                    component.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        if (isExtended()) {
            for (Component component : getComponents()) {
                if (component instanceof SettingComponent && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                    component.mouseReleased(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    public float getComponentsSize() {
        float size = 0;
        for (Component component : getComponents()) {
            if (component instanceof SettingComponent && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                size += component.getHeight();
            }
        }
        return size;
    }

    private void updatePositions() {
        float offsetY = getHeight();
        for (Component component : getComponents()) {
            if (component instanceof SettingComponent && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component).getSetting())) {
                component.setOffsetY(offsetY);
                component.moved(getPosX(), getPosY());
                offsetY += component.getHeight();
            }
        }
    }

    public Module getModule() { return module; }
    public ArrayList<Component> getComponents() { return components; }
}