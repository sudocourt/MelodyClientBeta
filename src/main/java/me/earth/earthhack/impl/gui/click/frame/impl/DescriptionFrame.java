package me.earth.earthhack.impl.gui.click.frame.impl;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.gui.click.frame.Frame;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.clickgui.ClickGui;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class DescriptionFrame extends Frame
{
    public static final ModuleCache<ClickGui> CLICK_GUI = Caches.getModule(ClickGui.class);

    private String description;

    public DescriptionFrame(float posX, float posY, float width, float height)
    {
        this("Description", posX, posY, width, height);
    }

    public DescriptionFrame(String label, float posX, float posY, float width, float height)
    {
        super(label, posX, posY, width, height);
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks)
    {
        if (description == null || !CLICK_GUI.get().description.getValue())
        {
            return;
        }

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 150);

        // 1. Вызываем наш кастомный градиентный и монолитный рендер из Frame.java
        super.drawScreen(context, mouseX, mouseY, partialTicks);

        // Рендер ушек Melody Client, если они включены
        if (CLICK_GUI.get().catEars.getValue()) {
            CategoryFrame.catEarsRender(context, getPosX(), getPosY(), getWidth());
        }

        // Высчитываем высоту текстового блока описания
        List<String> strings = Managers.TEXT.listFormattedStringToWidth(this.getDescription(), (int) this.getWidth() - 1);
        float textBlockHeight = 3 + (Managers.TEXT.getStringHeightI() + 1) * strings.size();

        float backgroundTop = getPosY() + getHeight();
        float backgroundBottom = backgroundTop + textBlockHeight + 1f;

        // 2. Рендерим сплошной темный фон монолита под текст описания (Cyberpunk серый)
        int bgColor = new java.awt.Color(15, 15, 15, 245).getRGB();
        Render2DUtil.drawRect(context.getMatrices(), getPosX(), backgroundTop, getPosX() + getWidth(), backgroundBottom, bgColor);

        // 3. Рисуем тонкие боковые рамки (Кислотно-лаймовый из настроек ClickGui)
        int sideColor = CLICK_GUI.get().topColorsetting.getValue().getRGB();
        Render2DUtil.drawRect(context.getMatrices(), getPosX(), backgroundTop, getPosX() + 1f, backgroundBottom, sideColor); // Левая нить
        Render2DUtil.drawRect(context.getMatrices(), getPosX() + getWidth() - 1f, backgroundTop, getPosX() + getWidth(), backgroundBottom, sideColor); // Правая нить
        Render2DUtil.drawRect(context.getMatrices(), getPosX(), backgroundBottom - 1f, getPosX() + getWidth(), backgroundBottom, sideColor); // Нижная грань фрейма

        // 4. Отрисовываем сам текст описания внутри монолита
        float y = this.getPosY() + 2 + (getHeight() / 2) + Managers.TEXT.getStringHeightI();
        for (String string : strings)
        {
            Managers.TEXT.drawStringWithShadow(context, string, (int) (this.getPosX() + 3), (int) y, CLICK_GUI.get().getTextColorDesc().getRGB());
            y += Managers.TEXT.getStringHeightI() + 1;
        }

        context.getMatrices().pop();
    }


    @Override
    public void mouseReleased(double mouseX, double mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        CLICK_GUI.get().descPosX.setValue(getPosX());
        CLICK_GUI.get().descPosY.setValue(getPosY());
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

}
