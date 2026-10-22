package pl.lampamc.target;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class LampaTarget implements ClientModInitializer {
    private static KeyBinding openKey;
    private static KeyBinding markKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lampatarget.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.lampatarget"));
        markKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lampatarget.mark", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.lampatarget"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Marks.tick();
            while (openKey.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new TargetScreen());
            }
            while (markKey.wasPressed()) {
                markUnderCrosshair(mc);
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> Hud.render(ctx));
        WorldRenderEvents.LAST.register(WorldDraw::render);
    }

    /** Wybiera gracza najbliżej linii celowania (do ok. 5 stopni, max 128 bloków). */
    private static void markUnderCrosshair(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f).normalize();
        AbstractClientPlayerEntity best = null;
        double bestDot = 0.9962;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            Vec3d to = p.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist < 0.5 || dist > 128) continue;
            double dot = to.normalize().dotProduct(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = p;
            }
        }
        if (best != null) {
            Marks.mark(best);
            mc.player.sendMessage(Text.literal("Oznaczono: " + best.getName().getString()), true);
        }
    }
}
