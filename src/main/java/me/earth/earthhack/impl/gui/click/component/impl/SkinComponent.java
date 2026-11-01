package me.earth.earthhack.impl.gui.click.component.impl;

import me.earth.earthhack.api.setting.settings.StringSetting;
import me.earth.earthhack.impl.gui.click.component.SettingComponent;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.player.fakeplayer.FakePlayer;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class SkinComponent extends SettingComponent<String, StringSetting> {
    private final FakePlayer module;
    private boolean extended;

    public SkinComponent(StringSetting setting, FakePlayer module, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(setting.getName(), posX, posY, offsetX, offsetY, width, height, setting);
        this.module = module;

        // Принудительно грузим список при открытии меню
        if (module.allSkins.isEmpty()) {
            module.loadAllSkins();
        }
    }

    private List<String> getAvailableSkins() {
        List<String> list = new ArrayList<>();
        list.add("Random");
        list.addAll(module.allSkins.keySet());
        return list;
    }

    private String truncate(String text, float maxWidth) {
        if (Managers.TEXT.getStringWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        float ellipsisWidth = Managers.TEXT.getStringWidth(ellipsis);
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Managers.TEXT.getStringWidth(sb.toString() + c) + ellipsisWidth > maxWidth) break;
            sb.append(c);
        }
        return sb + ellipsis;
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(context, mouseX, mouseY, partialTicks);

        Render2DUtil.drawRect(context.getMatrices(), getFinishedX(), getFinishedY(), getFinishedX() + getWidth(), getFinishedY() + 14, 0xFF222222);

        String displayValue = setting.getValue();
        String text = getLabel() + ": " + truncate(displayValue, getWidth() - 25);

        drawStringWithShadow(text, getFinishedX() + 5, getFinishedY() + 7f - (Managers.TEXT.getStringHeightI() >> 1), 0xFFFFFFFF);

        String arrow = extended ? "-" : "+";
        drawStringWithShadow(arrow, getFinishedX() + getWidth() - 10, getFinishedY() + 7f - (Managers.TEXT.getStringHeightI() >> 1), 0xFFAAAAAA);

        if (extended) {
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 400);

            List<String> skins = getAvailableSkins();
            float dropY = getFinishedY() + 14;

            Render2DUtil.drawRect(context.getMatrices(), getFinishedX(), dropY, getFinishedX() + getWidth(), dropY + (skins.size() * 14), 0xFF111111);

            for (String skinName : skins) {
                boolean hovered = Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX(), dropY, getWidth(), 14);

                int bg = hovered ? 0xFF333333 : 0xFF111111;
                if (skinName.equals(setting.getValue())) bg = 0xFF1A5C0A; // Темно-зеленый для выбранного

                Render2DUtil.drawRect(context.getMatrices(), getFinishedX(), dropY, getFinishedX() + getWidth(), dropY + 14, bg);

                drawStringWithShadow(truncate(skinName, getWidth() - 10), getFinishedX() + 5, dropY + 7f - (Managers.TEXT.getStringHeightI() >> 1), 0xFFFFFFFF);

                dropY += 14;
            }

            context.getMatrices().pop();
        }
    }

    @Override
    public float getHeight() {
        if (extended) {
            return 14 + (getAvailableSkins().size() * 14);
        }
        return 14;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            // Клик по шапке (закрыть/открыть)
            if (Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), 14)) {
                if (!extended && module.allSkins.isEmpty()) {
                    module.loadAllSkins();
                }
                extended = !extended;
                return;
            }

            // Клик по списку
            if (extended) {
                List<String> skins = getAvailableSkins();
                float dropY = getFinishedY() + 14;

                for (String skinName : skins) {
                    if (Render2DUtil.mouseWithinBounds(mouseX, mouseY, getFinishedX(), dropY, getWidth(), 14)) {
                        setting.setValue(skinName);
                        // УБРАЛИ extended = false; Теперь список остается открытым!
                        return;
                    }
                    dropY += 14;
                }
            }
        }
    }
}