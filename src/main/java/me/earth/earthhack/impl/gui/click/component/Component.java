package me.earth.earthhack.impl.gui.click.component;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.gui.click.component.impl.ModuleComponent;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.clickgui.ClickGui;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

public abstract class Component {
    public static final ModuleCache<ClickGui> CLICK_GUI = Caches.getModule(ClickGui.class);

    protected String label;
    protected float posX, posY;
    protected float offsetX, offsetY;
    protected float width, height;
    protected boolean extended;
    protected Supplier<String> description;

    public Component(String label, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        this.label = label;
        this.posX = posX;
        this.posY = posY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;
    }

    public void init() {}
    public void moved(float posX, float posY) {
        this.posX = posX;
        this.posY = posY;
    }

    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {}
    public void charTyped(char character, int keyCode) {}
    public void keyPressed(int keyCode) {}
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {}
    public void mouseReleased(double mouseX, double mouseY, int mouseButton) {}
    public void mouseScrolled(double mouseX, double mouseY, double scrollAmount) {}

    public String getLabel() { return label; }
    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public void setOffsetY(float offsetY) { this.offsetY = offsetY; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }

    public float getFinishedX() { return posX + offsetX; }
    public float getFinishedY() { return posY + offsetY; }

    public boolean isExtended() { return extended; }
    public void setExtended(boolean extended) { this.extended = extended; }

    public void setDescription(Supplier<String> description) { this.description = description; }
    public Supplier<String> getDescription() { return description; }

    public ModuleCache<ClickGui> getClickGui() {
        return CLICK_GUI;
    }

    protected void drawStringWithShadow(String text, float x, float y, int color) {
        Managers.TEXT.drawStringWithShadow(ModuleComponent.context, text, x, y, color);
    }
}