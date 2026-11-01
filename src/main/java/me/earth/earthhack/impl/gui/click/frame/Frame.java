package me.earth.earthhack.impl.gui.click.frame;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.gui.click.component.Component;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.clickgui.ClickGui;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;

public class Frame {
    public static final ModuleCache<ClickGui> CLICK_GUI = Caches.getModule(ClickGui.class);

    private static final int BODY_COLOR   = 0xF00F0F0F;
    private static final int TEXT_COLOR   = 0xFFFFFFFF;

    private final String label;
    private float posX, posY, lastPosX, lastPosY, width;
    private final float height;
    private boolean extended, dragging;
    private final ArrayList<Component> components = new ArrayList<>();
    private int scrollY;

    private float bodyHeightProgress = 0f;
    private float hoverProgress = 0f;
    private long lastFrameMs = System.currentTimeMillis();

    public Frame(String label, float posX, float posY, float width, float height) {
        this.label = label;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
    }

    public void init() { components.forEach(Component::init); }
    public void moved(float posX, float posY) { components.forEach(c -> c.moved(posX, posY)); }

    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastFrameMs) / 1000f, 0.1f);
        lastFrameMs = now;

        var window = MinecraftClient.getInstance().getWindow();

        if (isDragging()) {
            setPosX(mouseX + getLastPosX());
            setPosY(mouseY + getLastPosY());
            getComponents().forEach(c -> c.moved(getPosX(), getPosY() + getScrollY()));
        }

        clampToScreen(window);

        boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight());
        hoverProgress = MathHelper.lerp(Math.min(dt * 20f, 1f), hoverProgress, hovered ? 1f : 0f);
        bodyHeightProgress = MathHelper.lerp(Math.min(dt * 15f, 1f), bodyHeightProgress, isExtended() ? 1f : 0f);

        int accent = CLICK_GUI.get().color.getValue().getRGB();

        float x1 = getPosX();
        float y1 = getPosY();
        float x2 = x1 + getWidth();
        float y2 = y1 + getHeight();

        // 1. ТЕЛО (Рисуем первым)
        if (bodyHeightProgress > 0.001f && !getComponents().isEmpty()) {
            float totalHeight = computeTotalContentHeight();
            float fullBottom = y2 + totalHeight;
            float bottom = MathHelper.lerp(bodyHeightProgress, y2, fullBottom);

            Render2DUtil.drawRect(context.getMatrices(), x1, y2, x2, bottom, BODY_COLOR);

            if (bodyHeightProgress > 0.5f && !(this instanceof me.earth.earthhack.impl.gui.click.frame.impl.ModulesFrame)) {
                getComponents().forEach(c -> c.drawScreen(context, mouseX, mouseY, partialTicks));
            }
        }

        // 2. ШАПКА (Рисуем поверх тела)
        int headerColor = Render2DUtil.lerpColor(hoverProgress, accent, Render2DUtil.lighten(accent, 1.15f));
        Render2DUtil.drawRect(context.getMatrices(), x1, y1, x2, y2, headerColor);

        // 3. Текст шапки
        float textY = y1 + getHeight() / 2f - Managers.TEXT.getStringHeight() / 2f;
        Managers.TEXT.drawStringWithShadow(context, getLabel(), x1 + 6f, textY, TEXT_COLOR);
    }

    private void clampToScreen(net.minecraft.client.util.Window window) {
        if (getPosX() < 0) { setPosX(0); getComponents().forEach(c -> c.moved(getPosX(), getPosY() + getScrollY())); }
        if (getPosX() + getWidth() > window.getScaledWidth()) {
            setPosX(window.getScaledWidth() - getWidth());
            getComponents().forEach(c -> c.moved(getPosX(), getPosY() + getScrollY()));
        }
        if (getPosY() < 0) { setPosY(0); getComponents().forEach(c -> c.moved(getPosX(), getPosY() + getScrollY())); }
        if (getPosY() + getHeight() > window.getScaledHeight()) {
            setPosY(window.getScaledHeight() - getHeight());
            getComponents().forEach(c -> c.moved(getPosX(), getPosY() + getScrollY()));
        }
    }

    private float computeTotalContentHeight() {
        float total = 0f;
        for (Component component : getComponents()) {
            total += component.getHeight();
            if (component instanceof me.earth.earthhack.impl.gui.click.component.impl.ModuleComponent mc) {
                if (mc.isExtended()) {
                    total += mc.getComponentsSize();
                    total += 3f;
                }
            }
        }
        return total;
    }

    public void charTyped(char character, int keyCode) { if (isExtended()) getComponents().forEach(c -> c.charTyped(character, keyCode)); }
    public void keyPressed(int keyCode) { if (isExtended()) getComponents().forEach(c -> c.keyPressed(keyCode)); }
    public void mouseScrolled(double mouseX, double mouseY, double scrollAmount) { if (isExtended()) getComponents().forEach(c -> c.mouseScrolled(mouseX, mouseY, scrollAmount)); }

    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        final boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight());
        switch (mouseButton) {
            case 0 -> {
                if (hovered) {
                    setDragging(true);
                    setLastPosX((float) (getPosX() - mouseX * CLICK_GUI.get().guiScale.getValue()));
                    setLastPosY((float) (getPosY() - mouseY * CLICK_GUI.get().guiScale.getValue()));
                }
            }
            case 1 -> { if (hovered) setExtended(!isExtended()); }
            default -> {}
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0 && isDragging()) setDragging(false);
        if (isExtended()) getComponents().forEach(c -> c.mouseReleased(mouseX, mouseY, mouseButton));
    }

    // ВОТ ОН, ВОЗВРАЩЕННЫЙ МЕТОД!
    protected void drawStringWithShadow(DrawContext context, String text, float x, float y, int color) {
        Managers.TEXT.drawStringWithShadow(context, text, x, y, color);
    }

    public ArrayList<Component> getComponents() { return components; }
    public String getLabel() { return label; }
    public float getWidth() { return width * CLICK_GUI.get().guiScale.getValue(); }
    public float getHeight() { return height * CLICK_GUI.get().guiScale.getValue(); }
    public float getPosX() { return posX * CLICK_GUI.get().guiScale.getValue(); }
    public void setPosX(float posX) { this.posX = posX; }
    public float getPosY() { return posY * CLICK_GUI.get().guiScale.getValue(); }
    public void setPosY(float posY) { this.posY = posY; }
    public float getLastPosX() { return lastPosX; }
    public void setLastPosX(float lastPosX) { this.lastPosX = lastPosX; }
    public float getLastPosY() { return lastPosY; }
    public void setLastPosY(float lastPosY) { this.lastPosY = lastPosY; }
    public boolean isExtended() { return extended; }
    public void setExtended(boolean extended) { this.extended = extended; }
    public boolean isDragging() { return dragging; }
    public void setDragging(boolean dragging) { this.dragging = dragging; }
    public int getScrollY() { return scrollY; }
    public void setScrollY(int scrollY) { this.scrollY = scrollY; }
    public void setWidth(float width) { this.width = width; }
}