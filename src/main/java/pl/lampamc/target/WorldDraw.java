package pl.lampamc.target;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Rysuje tracery i ramki hitboxa oznaczonych graczy (widoczne przez ściany). */
public class WorldDraw {
    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || Marks.ACTIVE.isEmpty()) return;
        if (!Config.I.tracers && !Config.I.hitbox) return;

        List<PlayerEntity> targets = new ArrayList<>();
        for (Marks.Mark m : Marks.ACTIVE.values()) {
            PlayerEntity p = mc.world.getPlayerByUuid(m.id);
            if (p != null && p != mc.player) targets.add(p);
        }
        if (targets.isEmpty()) return;

        float td = ctx.tickCounter().getTickDelta(false);
        Camera cam = ctx.camera();
        Vec3d c = cam.getPos();
        MatrixStack.Entry e = ctx.matrixStack().peek();
        Matrix4f m = e.getPositionMatrix();

        int rgb = Config.rgb();
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.0f);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        Vec3d start = Vec3d.fromPolar(cam.getPitch(), cam.getYaw()).multiply(0.6);
        for (PlayerEntity p : targets) {
            Vec3d lp = p.getLerpedPos(td);
            Box box = p.getBoundingBox().offset(lp.x - p.getX(), lp.y - p.getY(), lp.z - p.getZ()).offset(-c.x, -c.y, -c.z);
            if (Config.I.tracers) line(buf, e, m, start, box.getCenter(), r, g, b, 1f);
            if (Config.I.hitbox) box(buf, e, m, box, r, g, b, 1f);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void box(BufferBuilder buf, MatrixStack.Entry e, Matrix4f m, Box b, float r, float g, float bl, float a) {
        double x1 = b.minX, y1 = b.minY, z1 = b.minZ, x2 = b.maxX, y2 = b.maxY, z2 = b.maxZ;
        // dół
        line(buf, e, m, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), r, g, bl, a);
        line(buf, e, m, new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), r, g, bl, a);
        line(buf, e, m, new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), r, g, bl, a);
        line(buf, e, m, new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), r, g, bl, a);
        // góra
        line(buf, e, m, new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), r, g, bl, a);
        line(buf, e, m, new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), r, g, bl, a);
        line(buf, e, m, new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), r, g, bl, a);
        line(buf, e, m, new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), r, g, bl, a);
        // słupki
        line(buf, e, m, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), r, g, bl, a);
        line(buf, e, m, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), r, g, bl, a);
        line(buf, e, m, new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), r, g, bl, a);
        line(buf, e, m, new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), r, g, bl, a);
    }

    private static void line(BufferBuilder buf, MatrixStack.Entry e, Matrix4f m, Vec3d a, Vec3d z, float r, float g, float b, float al) {
        Vec3d d = z.subtract(a);
        double len = d.length();
        if (len < 1.0e-6) return;
        d = d.multiply(1.0 / len);
        buf.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(r, g, b, al).normal(e, (float) d.x, (float) d.y, (float) d.z);
        buf.vertex(m, (float) z.x, (float) z.y, (float) z.z).color(r, g, b, al).normal(e, (float) d.x, (float) d.y, (float) d.z);
    }
}
