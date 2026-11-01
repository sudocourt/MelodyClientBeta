package me.earth.earthhack.impl.gui.click.component.impl;

import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.gui.click.component.SettingComponent;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class BooleanComponent extends SettingComponent<Boolean, BooleanSetting> {
    private final BooleanSetting booleanSetting;
    private float toggleProgress = 0f;

    public BooleanComponent(BooleanSetting booleanSetting, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(booleanSetting.getName(), posX, posY, offsetX, offsetY, width, height, booleanSetting);
        this.booleanSetting = booleanSetting;
        this.toggleProgress = booleanSetting.getValue() ? 1f : 0f;
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(context, mouseX, mouseY, partialTicks);
        final boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX() + getWidth() - 20, getFinishedY() + 1, 16, getHeight() - 2);

        float target = getBooleanSetting().getValue() ? 1f : 0f;
        toggleProgress = MathHelper.lerp(partialTicks * 0.4f, toggleProgress, target);

        int textColor = getBooleanSetting().getValue() ? 0xFFFFFFFF : 0xFFAAAAAA;
        if (hovered) textColor = Render2DUtil.lighten(textColor, 1.2f);
        drawStringWithShadow(getLabel(), getFinishedX() + 5, getFinishedY() + getHeight() / 2 - (Managers.TEXT.getStringHeightI() >> 1), textColor);

        // Квадратный Switch
        float switchX = getFinishedX() + getWidth() - 20;
        float switchY = getFinishedY() + getHeight() / 2f - 4;
        int bgOff = 0xFF333333;
        int bgOn = getClickGui().get().getModulesColor().getRGB();
        int bgColor = Render2DUtil.lerpColor(toggleProgress, bgOff, bgOn);

        Render2DUtil.drawRect(context.getMatrices(), switchX, switchY, switchX + 15, switchY + 8, bgColor);

        // Квадратная ручка
        float circleX = MathHelper.lerp(toggleProgress, switchX + 1f, switchX + 8f);
        Render2DUtil.drawRect(context.getMatrices(), circleX, switchY + 1, circleX + 6, switchY + 7, 0xFFFFFFFF);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX() + getWidth() - 20, getFinishedY() + 1, 16, getHeight() - 2);
        if (hovered && mouseButton == 0)
            getBooleanSetting().setValue(!getBooleanSetting().getValue());
    }

    public BooleanSetting getBooleanSetting() {
        return booleanSetting;
    }
}