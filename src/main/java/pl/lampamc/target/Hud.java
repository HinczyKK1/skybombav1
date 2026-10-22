package pl.lampamc.target;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

public class Hud {
    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.I.hud || mc.player == null || mc.world == null || mc.options.hudHidden) return;
        int y = 10;
        for (Marks.Mark m : Marks.ACTIVE.values()) {
            long rem = Marks.remaining(m);
            PlayerEntity p = mc.world.getPlayerByUuid(m.id);
            String hp = p == null ? "poza zasięgiem" : "♥ " + (int) Math.ceil(p.getHealth());
            int w = 150, h = 30;
            ctx.fill(10, y, 10 + w, y + h, 0xCC0D0D14);
            ctx.fill(10, y, 12, y + h, Config.argb());
            ctx.drawText(mc.textRenderer, m.name, 18, y + 4, 0xFFFFFFFF, true);
            ctx.drawText(mc.textRenderer, hp + "   " + Marks.fmt(rem), 18, y + 15, 0xFFAAAAAA, false);
            double frac = Math.max(0, Math.min(1, rem / (double) m.dur));
            ctx.fill(12, y + h - 3, 12 + (int) ((w - 2) * frac), y + h, Config.argb());
            y += h + 4;
        }
    }
}
