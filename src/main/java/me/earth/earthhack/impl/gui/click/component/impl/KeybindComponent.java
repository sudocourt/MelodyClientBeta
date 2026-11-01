package me.earth.earthhack.impl.gui.click.component.impl;

import me.earth.earthhack.api.setting.settings.BindSetting;
import me.earth.earthhack.api.util.bind.Bind;
import me.earth.earthhack.impl.gui.click.component.SettingComponent;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import me.earth.earthhack.pingbypass.input.Keyboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class KeybindComponent extends SettingComponent<Bind, BindSetting> {
    private final BindSetting bindSetting;
    private boolean binding;

    public KeybindComponent(BindSetting bindSetting, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(bindSetting.getName(), posX, posY, offsetX, offsetY, width, height, bindSetting);
        this.bindSetting = bindSetting;
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(context, mouseX, mouseY, partialTicks);
        final boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX() + 5, getFinishedY() + 1, getWidth() - 10, getHeight() - 2);

        int bgColor = hovered ? 0x40FFFFFF : 0x20FFFFFF;
        if (isBinding()) {
            float pulse = (MathHelper.sin(System.currentTimeMillis() / 150f) + 1f) / 2f;
            bgColor = Render2DUtil.withAlpha(getClickGui().get().getSettingColor().getRGB(), (int)(50 + pulse * 100));
        }

        // Квадратный фон кнопки
        Render2DUtil.drawRect(context.getMatrices(), getFinishedX() + 5, getFinishedY() + 1, getFinishedX() + getWidth() - 5, getFinishedY() + getHeight() - 1, bgColor);

        String text = isBinding() ? "Press a key..." : getBindSetting().getName() + ": " + Formatting.GRAY + getBindSetting().getValue();
        drawStringWithShadow(text, getFinishedX() + 7, getFinishedY() + getHeight() / 2f - (Managers.TEXT.getStringHeightI() >> 1), 0xFFFFFFFF);
    }

    @Override
    public void keyPressed(int keyCode) {
        super.keyPressed(keyCode);
        if (isBinding()) {
            final Bind bind = Bind.fromKey(keyCode == Keyboard.getEscape() || keyCode == Keyboard.getSpace() || keyCode == Keyboard.getDelete() ? Keyboard.getNone() : keyCode);
            getBindSetting().setValue(bind);
            setBinding(false);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX() + 5, getFinishedY() + 1, getWidth() - 10, getHeight() - 2);
        if (hovered && mouseButton == 0) setBinding(!isBinding());
    }

    public BindSetting getBindSetting() { return bindSetting; }
    public boolean isBinding() { return binding; }
    public void setBinding(boolean binding) { this.binding = binding; }
}