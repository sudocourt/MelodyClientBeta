package me.earth.earthhack.impl.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

import java.awt.*;

public class Render3DUtil {

    public static void drawBox(MatrixStack stack, Box box, Color c) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        VertexConsumer buffer = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayer.getDebugFilledBox());

        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void drawOutline(MatrixStack stack, Box box, Color color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        VertexConsumer buffer = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayer.getLines());

        Box inflated = box.expand(0.01);
        float x1 = (float) inflated.minX, y1 = (float) inflated.minY, z1 = (float) inflated.minZ;
        float x2 = (float) inflated.maxX, y2 = (float) inflated.maxY, z2 = (float) inflated.maxZ;
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}