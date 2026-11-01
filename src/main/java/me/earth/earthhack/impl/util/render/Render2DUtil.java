package me.earth.earthhack.impl.util.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.util.TextUtil;
import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.managers.render.TextRenderer;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.editor.HudEditor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import net.minecraft.client.render.*;

import java.awt.*;

public class Render2DUtil implements Globals {

    // ============ Корнер-маски для roundedRect ============
    public static final int CORNER_TL = 1;
    public static final int CORNER_TR = 2;
    public static final int CORNER_BR = 4;
    public static final int CORNER_BL = 8;
    public static final int CORNER_ALL = CORNER_TL | CORNER_TR | CORNER_BR | CORNER_BL;
    public static final int CORNER_TOP = CORNER_TL | CORNER_TR;
    public static final int CORNER_BOTTOM = CORNER_BL | CORNER_BR;

    private static final ModuleCache<HudEditor> HUD_EDITOR = Caches.getModule(HudEditor.class);

    // ============ Frame clock для плавных анимаций ============
    private static long lastFrameNs = 0L;
    private static float frameDelta = 1f / 60f;

    /**
     * Вызови один раз в начале кадра (например, в Click#drawScreen),
     * чтобы все анимации были frame-rate-independent.
     */
    public static void updateFrameClock() {
        long now = System.nanoTime();
        if (lastFrameNs != 0L) {
            frameDelta = Math.min((now - lastFrameNs) / 1_000_000_000f, 0.1f);
        }
        lastFrameNs = now;
    }

    public static float frameDelta() {
        return frameDelta;
    }

    public static float animate(float current, float target, float speed) {
        return MathHelper.lerp(Math.min(frameDelta * speed, 1f), current, target);
    }

    public static void drawBlurryRect(MatrixStack matrix, float x, float y, float x1, float y1, int intensity, float size) {
        // Временная заглушка для совместимости.
        // В 1.21.1 настоящий блюр делается через PostChain (шейдеры).
        drawRect(matrix, x, y, x1, y1, new Color(0, 0, 0, 100).getRGB());
    }

    public static double getScreenScale() {
        return mc.getWindow().getScaleFactor();
    }

    public static int getScreenWidth() {
        return mc.getWindow().getScaledWidth();
    }

    public static int getScreenHeight() {
        return mc.getWindow().getScaledHeight();
    }

    // ============ Цвета ============
    public static int lerpColor(float t, int a, int b) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = (int) (aa + (ba - aa) * t);
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }

    // ============ Базовая заливка ============
    public static void drawRect(MatrixStack matrix, float startX, float startY, float endX, float endY, int color) {
        drawRect(matrix, startX, startY, endX, endY, color, 0);
    }

    public static void drawRect(MatrixStack matrix, float startX, float startY, float endX, float endY, int color, int zLevel) {
        if (Managers.TEXT.usingCustomFont()) {
            TextRenderer.FONTS.drawRect(startX, startY, endX, endY, color);
            return;
        }
        if (endX <= startX || endY <= startY) return;

        Matrix4f posMatrix = matrix.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bb.vertex(posMatrix, startX, endY, zLevel).color(color);
        bb.vertex(posMatrix, endX, endY, zLevel).color(color);
        bb.vertex(posMatrix, endX, startY, zLevel).color(color);
        bb.vertex(posMatrix, startX, startY, zLevel).color(color);
        BufferRenderer.drawWithGlobalProgram(bb.end());

        RenderSystem.disableBlend();
    }

    // ============ Градиент ============
    public static void drawGradientRect(MatrixStack matrix, float left, float top, float right, float bottom,
                                        boolean sideways, int startColor, int endColor) {
        if (Managers.TEXT.usingCustomFont()) {
            TextRenderer.FONTS.drawGradientRect(left, top, right - left, bottom - top, startColor, endColor);
            return;
        }
        if (right <= left || bottom <= top) return;

        float f = (startColor >>> 24 & 255) / 255f;
        float f1 = (startColor >> 16 & 255) / 255f;
        float f2 = (startColor >> 8 & 255) / 255f;
        float f3 = (startColor & 255) / 255f;
        float f4 = (endColor >>> 24 & 255) / 255f;
        float f5 = (endColor >> 16 & 255) / 255f;
        float f6 = (endColor >> 8 & 255) / 255f;
        float f7 = (endColor & 255) / 255f;

        Matrix4f posMatrix = matrix.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        if (sideways) {
            bb.vertex(posMatrix, left, top, 0f).color(f1, f2, f3, f);
            bb.vertex(posMatrix, left, bottom, 0f).color(f1, f2, f3, f);
            bb.vertex(posMatrix, right, bottom, 0f).color(f5, f6, f7, f4);
            bb.vertex(posMatrix, right, top, 0f).color(f5, f6, f7, f4);
        } else {
            bb.vertex(posMatrix, right, top, 0f).color(f1, f2, f3, f);
            bb.vertex(posMatrix, left, top, 0f).color(f1, f2, f3, f);
            bb.vertex(posMatrix, left, bottom, 0f).color(f5, f6, f7, f4);
            bb.vertex(posMatrix, right, bottom, 0f).color(f5, f6, f7, f4);
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderSystem.disableBlend();
    }

    // ============ Правильные скругления через TRIANGLE_FAN ============
    public static void roundedRect(MatrixStack matrix, float x, float y, float x2, float y2, float radius, int color) {
        roundedRect(matrix, x, y, x2, y2, radius, color, CORNER_ALL);
    }

    public static void roundedRect(MatrixStack matrix, float x, float y, float x2, float y2,
                                   float radius, int color, int corners) {
        if (Managers.TEXT.usingCustomFont() && corners == CORNER_ALL) {
            TextRenderer.FONTS.drawRoundedRect(x, y, x2, y2, radius, color);
            return;
        }
        if (x2 <= x || y2 <= y) return;

        float r = Math.max(0f, Math.min(radius, Math.min(x2 - x, y2 - y) / 2f));
        if (r <= 0f) {
            drawRect(matrix, x, y, x2, y2, color);
            return;
        }

        boolean tl = (corners & CORNER_TL) != 0;
        boolean tr = (corners & CORNER_TR) != 0;
        boolean br = (corners & CORNER_BR) != 0;
        boolean bl = (corners & CORNER_BL) != 0;

        float topInset = (tl || tr) ? r : 0f;
        float botInset = (bl || br) ? r : 0f;
        float leftInset = (tl || bl) ? r : 0f;
        float rightInset = (tr || br) ? r : 0f;

        // Центральная полоса (по всей высоте, но с отступами слева/справа)
        drawRect(matrix, x + leftInset, y, x2 - rightInset, y2, color);
        // Боковые полосы (между верхним и нижним скруглением)
        if (leftInset > 0f) drawRect(matrix, x, y + topInset, x + leftInset, y2 - botInset, color);
        if (rightInset > 0f) drawRect(matrix, x2 - rightInset, y + topInset, x2, y2 - botInset, color);

        // Углы через TRIANGLE_FAN
        if (tl) drawCornerFan(matrix, x + r, y + r, r, color, 180f, 270f);
        if (tr) drawCornerFan(matrix, x2 - r, y + r, r, color, 270f, 360f);
        if (br) drawCornerFan(matrix, x2 - r, y2 - r, r, color, 0f, 90f);
        if (bl) drawCornerFan(matrix, x + r, y2 - r, r, color, 90f, 180f);
    }

    private static void drawCornerFan(MatrixStack matrix, float cx, float cy, float radius, int color, float startDeg, float endDeg) {
        Matrix4f posMatrix = matrix.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        bb.vertex(posMatrix, cx, cy, 0f).color(color);
        int segments = Math.max(4, (int) Math.ceil(radius * 2f));
        for (int i = 0; i <= segments; i++) {
            float deg = startDeg + (endDeg - startDeg) * i / (float) segments;
            float rad = (float) Math.toRadians(deg);
            bb.vertex(posMatrix, cx + (float) Math.cos(rad) * radius,
                    cy + (float) Math.sin(rad) * radius, 0f).color(color);
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderSystem.disableBlend();
    }

    // ============ Круг ============
    public static void drawCircle(MatrixStack matrix, float cx, float cy, float radius, int color) {
        drawCornerFan(matrix, cx, cy, radius, color, 0f, 360f);
    }
    // ============ Утилиты для работы с цветом ============
    public static int darker(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lighten(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    /** @deprecated используй roundedRect с corner-маской */
    @Deprecated
    public static void drawQuarterCircle(MatrixStack matrix, float x, float y, float radius, int color, int position) {
        // обратная совместимость: position 1..4 = BR, TR, TL, BL
        float start = switch (position) {
            case 1 -> 0f;    // BR
            case 2 -> 270f;  // TR
            case 3 -> 180f;  // TL
            default -> 90f;  // BL
        };
        float cx = x, cy = y;
        switch (position) {
            case 1 -> { cx = x - radius; cy = y - radius; }
            case 2 -> { cx = x - radius; cy = y + radius; }
            case 3 -> { cx = x + radius; cy = y + radius; }
            case 4 -> { cx = x + radius; cy = y - radius; }
        }
        drawCornerFan(matrix, cx, cy, radius, color, start, start + 90f);
    }

    // ============ Обводка ============
    public static void drawBorderedRect(MatrixStack matrix, float x, float y, float x2, float y2,
                                        float lineSize, int color, int borderColor) {
        drawRect(matrix, x, y, x2, y2, color);
        drawRect(matrix, x, y, x + lineSize, y2, borderColor);
        drawRect(matrix, x2 - lineSize, y, x2, y2, borderColor);
        drawRect(matrix, x, y2 - lineSize, x2, y2, borderColor);
        drawRect(matrix, x, y, x2, y + lineSize, borderColor);
    }

    // ============ Тень (стек слоёв) ============
    public static void drawShadow(MatrixStack matrix, float x, float y, float x2, float y2,
                                  float radius, int shadowColor, int layers) {
        int baseAlpha = (shadowColor >>> 24) & 0xFF;
        for (int i = layers; i >= 1; i--) {
            float spread = i * 0.9f;
            int alpha = Math.max(1, baseAlpha / (i * 2));
            int c = (alpha << 24) | (shadowColor & 0xFFFFFF);
            roundedRect(matrix, x - spread, y - spread, x2 + spread, y2 + spread, radius + spread, c);
        }
    }

    // ============ Прогресс-бар ============
    public static void progressBar(MatrixStack matrix, float startX, float endX, float y, float thickness, int color) {
        float r = thickness / 2f;
        roundedRect(matrix, startX, y - r, endX, y + r, r, color);
    }

    public static void progressBar(MatrixStack matrix, float startX, float endX, float y, float thickness,
                                   int colorLeft, int colorRight) {
        float r = thickness / 2f;
        float midX = (startX + endX) / 2f;
        roundedRect(matrix, startX, y - r, midX, y + r, r, colorLeft, CORNER_TL | CORNER_BL);
        roundedRect(matrix, midX, y - r, endX, y + r, r, colorRight, CORNER_TR | CORNER_BR);
        drawRect(matrix, midX - 1, y - r, midX + 1, y + r, (colorLeft + colorRight) / 2);
    }

    // ============ Линия ============
    public static void drawLine(MatrixStack matrix, float x, float y, float x1, float y1, float lineWidth, int color) {
        if (Managers.TEXT.usingCustomFont()) {
            TextRenderer.FONTS.drawLine(x, y, x1, y1, lineWidth, color);
            return;
        }
        Matrix4f posMatrix = matrix.peek().getPositionMatrix();

        float dx = x1 - x, dy = y1 - y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0.0001f) return;
        float nx = dx / len, ny = -(dy / len);
        float w = lineWidth / 2f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bb.vertex(posMatrix, x + ny * w, y + nx * w, 0f).color(color);
        bb.vertex(posMatrix, x1 + ny * w, y1 + nx * w, 0f).color(color);
        bb.vertex(posMatrix, x1 - ny * w, y1 - nx * w, 0f).color(color);
        bb.vertex(posMatrix, x - ny * w, y - nx * w, 0f).color(color);
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderSystem.disableBlend();
    }

    public static void drawCheckMark(MatrixStack matrix, float x, float y, int width, int color) {
        drawLine(matrix, x + width - 6.5f, y + 3f, x + width - 11.5f, y + 10f, 0.8f, color);
        drawLine(matrix, x + width - 11.5f, y + 10f, x + width - 13.5f, y + 8, 0.8f, color);
    }

    public static void drawCheckeredBackground(MatrixStack matrix, float x, float y, float x2, float y2) {
        drawRect(matrix, x, y, x2, y2, 0xFFFFFFFF);
        boolean offset = false;
        for (float yy = y; yy < y2; yy++) {
            for (float xx = x + ((offset = !offset) ? 1 : 0); xx < x2; xx += 2) {
                if (xx > x2 - 1) continue;
                drawRect(matrix, xx, yy, xx + 1, yy + 1, 0xFF808080);
            }
        }
    }

    // ============ Blur — использует ванильный фон (заглушка, если нужен — см. коммент) ============
    public static void blurArea(int x, int y, int width, int height, float intensity, float blurWidth, float blurHeight) {
        // Для настоящего гаусса нужен PostChain (шейдеры). Простейший визуальный суррогат:
        // слой очень тёмного полупрозрачного прямоугольника поверх фона.
        // Если хочешь настоящий blur — подключай PostChain в Click#render и не забудь resources.
    }

    // ============ Scissor ============
    public static void scissor(DrawContext context, float x, float y, float x1, float y1) {
        int minX = (int) Math.floor(Math.min(x, x1));
        int minY = (int) Math.floor(Math.min(y, y1));
        int maxX = (int) Math.ceil(Math.max(x, x1));
        int maxY = (int) Math.ceil(Math.max(y, y1));

        if (Managers.TEXT.usingCustomFont()) {
            double f = mc.getWindow().getScaleFactor();
            double h = mc.getWindow().getFramebufferHeight();
            int px = (int) Math.round(minX * f);
            int py = (int) Math.round(h - f * maxY);
            int pw = (int) Math.round(f * (maxX - minX));
            int ph = (int) Math.round(f * (maxY - minY));
            TextRenderer.FONTS.enableScissors(px, py, pw, ph);
        } else {
            context.enableScissor(minX, minY, maxX, maxY);
        }
    }

    public static void disableScissor(DrawContext context) {
        if (Managers.TEXT.usingCustomFont()) {
            TextRenderer.FONTS.disableScissors();
        } else {
            context.disableScissor();
        }
    }

    // ============ Прочее ============
    public static void drawPlayerFace(DrawContext context, PlayerEntity player, int x, int y, int width, int height) {
        if (player == null || mc.player == null || mc.player.networkHandler == null) return;
        PlayerListEntry entry = mc.player.networkHandler.getPlayerList().stream()
                .filter(info -> info.getProfile().getName().equals(player.getName().getString()))
                .findFirst().orElse(null);
        if (entry == null) return;
        Identifier tex = entry.getSkinTextures().texture();
        if (tex == null) return;
        context.drawTexture(tex, x, y, 8, 8, width, height, 64, 64);
    }

    public static void drawPlayer(DrawContext context, PlayerEntity player, int playerScale, int x, int y, int width, int height) {
        InventoryScreen.drawEntity(context, x, y, x + width, y + height, Math.max(10, playerScale - 10), 0,
                (float) mc.mouse.getX(), (float) mc.mouse.getY(), player);
    }

    public static void drawItem(DrawContext context, ItemStack itemStack, int x, int y, boolean amount) {
        drawItem(context, itemStack, x, y, amount, false);
    }

    public static void drawItem(DrawContext context, ItemStack itemStack, int x, int y, boolean amount, boolean countUnstackable) {
        if (itemStack == null || itemStack.isEmpty()) return;
        int count = itemStack.getCount();
        context.getMatrices().push();
        context.getMatrices().translate(0f, 0f, 200f);
        context.drawItem(itemStack, x, y);
        if (amount && count > 1) {
            context.getMatrices().translate(0f, 0f, 201f);
            String text = TextUtil.numberFormatter(count);
            Managers.TEXT.drawString(context, text,
                    x + 17 - Managers.TEXT.getStringWidth(text), y + 9,
                    HUD_EDITOR.get().matchColor.getValue() ? HUD_EDITOR.get().color.getValue().getRGB() : 0xFFFFFFFF,
                    true);
        }
        context.getMatrices().pop();
    }

    public static void drawItemsInventory(DrawContext context, DefaultedList<ItemStack> items, int x, int y) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                drawItem(context, stack, x + (i % 9) * 18, y + (i / 9) * 18, true);
            }
        }
    }

    public static boolean mouseWithinBounds(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}