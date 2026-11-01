package me.earth.earthhack.impl.gui.click.frame.impl;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.gui.click.component.Component;
import me.earth.earthhack.impl.gui.click.component.SettingComponent;
import me.earth.earthhack.impl.gui.click.component.impl.ModuleComponent; // <--- ВОТ ЭТОТ ИМПОРТ ПОТЕРЯЛСЯ
import me.earth.earthhack.impl.gui.click.frame.Frame;
import me.earth.earthhack.impl.gui.visibility.Visibilities;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.clickgui.ClickGui;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import static me.earth.earthhack.api.util.interfaces.Globals.mc;

public class ModulesFrame extends Frame {
    private static final ModuleCache<ClickGui> CLICK_GUI = Caches.getModule(ClickGui.class);

    // Переменные для плавного скроллинга
    private float targetScrollY = 0f;
    private float currentScrollY = 0f;

    public ModulesFrame(String name, float posX, float posY, float width, float height) {
        super(name, posX, posY, width, height);
        this.setExtended(true);
    }

    @Override
    public void moved(float posX, float posY) {
        super.moved(posX, posY);
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        // Плавная интерполяция скролла
        currentScrollY = MathHelper.lerp(partialTicks * 0.6f, currentScrollY, targetScrollY);
        if (Math.abs(currentScrollY - targetScrollY) < 0.5f) {
            currentScrollY = targetScrollY;
        }

        super.drawScreen(context, mouseX, mouseY, partialTicks);

        final float scrollMaxHeight = mc.getWindow().getScaledHeight();

        if (CLICK_GUI.get().catEars.getValue()) {
            CategoryFrame.catEarsRender(context, getPosX(), getPosY(), getWidth());
        }

        if (CLICK_GUI.get().size.getValue()) {
            String disString = "[" + getComponents().size() + "]";
            Managers.TEXT.drawStringWithShadow(context, disString, (getPosX() + getWidth() - 6 - Managers.TEXT.getStringWidth(disString)), (getPosY() + getHeight() / 2 - (Managers.TEXT.getStringHeightI() >> 1)), 0xFFFFFFFF);
        }

        if (isExtended()) {
            // Ограничитель скролла
            float contentHeight = getScrollCurrentHeight();
            if (contentHeight > scrollMaxHeight) {
                float minScroll = -(contentHeight - scrollMaxHeight);
                if (targetScrollY > 0) targetScrollY = 0;
                if (targetScrollY < minScroll) targetScrollY = minScroll;
            } else {
                targetScrollY = 0;
            }

            getComponents().forEach(component -> component.drawScreen(context, mouseX, mouseY, partialTicks));
        }
        updatePositions();
    }

    @Override
    public int getScrollY() {
        return (int) currentScrollY;
    }

    @Override
    public void setScrollY(int scrollY) {
        this.targetScrollY = scrollY;
        this.currentScrollY = scrollY;
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        super.mouseScrolled(mouseX, mouseY, scrollAmount);
        if (isExtended()) {
            final float scrollMaxHeight = mc.getWindow().getScaledHeight();
            if (Render2DUtil.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY() + getHeight(), getWidth(), (Math.min(getScrollCurrentHeight(), scrollMaxHeight)) + 1) && getScrollCurrentHeight() > scrollMaxHeight) {
                final float scrollSpeed = (CLICK_GUI.get().scrollSpeed.getValue() >> 2);
                if (scrollAmount < 0) {
                    targetScrollY -= scrollSpeed * Math.abs(scrollAmount);
                } else if (scrollAmount > 0) {
                    targetScrollY += scrollSpeed * scrollAmount;
                }
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final float scrollMaxHeight = MinecraftClient.getInstance().getWindow().getScaledHeight() - getHeight();
        if (isExtended() && Render2DUtil.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY() + getHeight(), getWidth(), (Math.min(getScrollCurrentHeight(), scrollMaxHeight)) + 1))
            getComponents().forEach(component -> component.mouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    private void updatePositions() {
        float offsetY = getHeight();
        for (Component component : getComponents()) {
            component.setOffsetY(offsetY);
            component.moved(getPosX(), getPosY() + getScrollY());
            if (component instanceof ModuleComponent) {
                if (component.isExtended()) {
                    for (Component component1 : ((ModuleComponent) component).getComponents()) {
                        if (component1 instanceof SettingComponent
                                && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component1).getSetting())) {
                            offsetY += component1.getHeight();
                        }
                    }
                    offsetY += 3.f;
                }
            }
            offsetY += component.getHeight();
        }
    }

    private float getScrollCurrentHeight() {
        return getCurrentHeight() + getHeight() + 3.f;
    }

    private float getCurrentHeight() {
        float cHeight = 1;
        for (Component component : getComponents()) {
            if (component instanceof ModuleComponent) {
                if (component.isExtended()) {
                    for (Component component1 : ((ModuleComponent) component).getComponents()) {
                        if (component1 instanceof SettingComponent
                                && Visibilities.VISIBILITY_MANAGER.isVisible(((SettingComponent<?, ?>) component1).getSetting())) {
                            cHeight += component1.getHeight();
                        }
                    }
                    cHeight += 3.f;
                }
            }
            cHeight += component.getHeight();
        }
        return cHeight;
    }
}