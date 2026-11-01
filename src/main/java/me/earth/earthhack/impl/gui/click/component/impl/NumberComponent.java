package me.earth.earthhack.impl.gui.click.component.impl;

import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.gui.click.component.SettingComponent;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.math.MathUtil;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class NumberComponent extends SettingComponent<Number, NumberSetting<Number>> {
    private final NumberSetting<Number> numberSetting;
    private boolean sliding;
    private float renderProgress;

    public NumberComponent(NumberSetting<Number> numberSetting, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(numberSetting.getName(), posX, posY, offsetX, offsetY, width, height, numberSetting);
        this.numberSetting = numberSetting;
        this.renderProgress = getProgress();
    }

    private float getProgress() {
        return (getNumberSetting().getValue().floatValue() - getNumberSetting().getMin().floatValue()) /
                (getNumberSetting().getMax().floatValue() - getNumberSetting().getMin().floatValue());
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(context, mouseX, mouseY, partialTicks);
        final boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());

        drawStringWithShadow(getLabel() + ": " + Formatting.GRAY + getNumberSetting().getValue(), getFinishedX() + 5, getFinishedY() + getHeight() / 2 - (Managers.TEXT.getStringHeightI() >> 1), 0xFFFFFFFF);

        renderProgress = MathHelper.lerp(partialTicks * 0.5f, renderProgress, getProgress());

        float sliderY = getFinishedY() + getHeight() - 3f;
        float sliderWidth = getWidth() - 10;
        float filledWidth = sliderWidth * renderProgress;

        // Фон слайдера (квадратный)
        Render2DUtil.drawRect(context.getMatrices(), getFinishedX() + 5, sliderY, getFinishedX() + 5 + sliderWidth, sliderY + 2f, 0xFF333333);

        // Заполнение
        int color = hovered || sliding ? getClickGui().get().getSettingColor().brighter().getRGB() : getClickGui().get().getSettingColor().getRGB();
        if (filledWidth > 0.5f) {
            Render2DUtil.drawRect(context.getMatrices(), getFinishedX() + 5, sliderY, getFinishedX() + 5 + filledWidth, sliderY + 2f, color);
        }

        // Квадратная ручка
        Render2DUtil.drawRect(context.getMatrices(), getFinishedX() + 5 + filledWidth - 1.5f, sliderY - 1, getFinishedX() + 5 + filledWidth + 1.5f, sliderY + 3f, 0xFFFFFFFF);

        if (sliding) {
            double val = ((mouseX - (getFinishedX() + 5)) * (getNumberSetting().getMax().doubleValue() - getNumberSetting().getMin().doubleValue()) / sliderWidth + getNumberSetting().getMin().doubleValue());
            getNumberSetting().setValue(getNumberSetting().numberToValue(MathUtil.round(val, 2)));
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());
        if (hovered && mouseButton == 0)
            setSliding(true);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        if (isSliding()) setSliding(false);
    }

    public NumberSetting<Number> getNumberSetting() { return numberSetting; }
    public boolean isSliding() { return sliding; }
    public void setSliding(boolean sliding) { this.sliding = sliding; }
}