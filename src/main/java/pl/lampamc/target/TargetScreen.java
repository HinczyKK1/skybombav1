package pl.lampamc.target;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TargetScreen extends Screen {
    private static final String[] TABS = {"Gracze", "Atak", "Historia", "Ustawienia", "Motywy"};
    private static final int W = 460, H = 290, SB = 96, ROW = 38;

    private int tab = 0, scroll = 0, px, py;
    private TextFieldWidget search;
    private final List<Object[]> hits = new ArrayList<>();
    private int clipTop = Integer.MIN_VALUE, clipBottom = Integer.MAX_VALUE;

    public TargetScreen() {
        super(Text.literal("LampaTarget"));
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        String old = search == null ? "" : search.getText();
        search = new TextFieldWidget(textRenderer, px + SB + 10, py + 34, W - SB - 20, 18, Text.literal("Szukaj"));
        search.setPlaceholder(Text.literal("Wyszukaj gracza..."));
        search.setText(old);
        addDrawableChild(search);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // wyłączamy domyślne rozmycie tła - rysujemy własne
    @Override
    public void renderBackground(DrawContext c, int mx, int my, float delta) {
    }

    // ---------- helpery UI ----------

    private void hit(int x1, int y1, int x2, int y2, Runnable r) {
        int a = Math.max(y1, clipTop), b = Math.min(y2, clipBottom);
        if (b > a) hits.add(new Object[]{x1, a, x2, b, r});
    }

    private void button(DrawContext c, int x, int y, int w, int h, String label, int mx, int my, boolean primary, Runnable r) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        int accent = Config.argb();
        int bg = primary ? (hov ? accent : ((accent & 0x00FFFFFF) | 0xCC000000)) : (hov ? 0xFF2F2F3D : 0xFF222230);
        c.fill(x, y, x + w, y + h, bg);
        c.drawText(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + (h - 8) / 2, 0xFFFFFFFF, false);
        hit(x, y, x + w, y + h, r);
    }

    private void toggle(DrawContext c, int x0, int x1, int y, String label, boolean on, Runnable flip) {
        c.fill(x0, y, x1, y + 24, 0xFF16161F);
        c.drawText(textRenderer, label, x0 + 8, y + 8, 0xFFDDDDE8, false);
        int pw = 30, ph = 14, pxx = x1 - pw - 10, pyy = y + 5;
        c.fill(pxx, pyy, pxx + pw, pyy + ph, on ? Config.argb() : 0xFF2F2F3D);
        int kx = on ? pxx + pw - ph + 2 : pxx + 2;
        c.fill(kx, pyy + 2, kx + ph - 4, pyy + ph - 2, 0xFFFFFFFF);
        hit(x0, y, x1, y + 24, flip);
    }

    private static String gear(PlayerEntity p) {
        int n = 0;
        String mat = "";
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot s : slots) {
            ItemStack st = p.getEquippedStack(s);
            if (!st.isEmpty()) {
                n++;
                if (mat.isEmpty()) {
                    String id = Registries.ITEM.getId(st.getItem()).getPath();
                    int i = id.lastIndexOf('_');
                    mat = i > 0 ? id.substring(0, i) : id;
                }
            }
        }
        String armor = n == 0 ? "Brak zbroi" : "Zbroja " + mat + " (" + n + "/4)";
        ItemStack hand = p.getMainHandStack();
        String h = hand.isEmpty() ? "puste ręce" : hand.getName().getString();
        return armor + " · " + h;
    }

    // ---------- render ----------

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        hits.clear();
        int accent = Config.argb();
        c.fill(0, 0, width, height, 0x99000000);
        c.fill(px, py, px + W, py + H, 0xF0101018);
        c.fill(px, py, px + SB, py + H, 0xF00B0B11);
        c.drawText(textRenderer, "LampaTarget", px + 12, py + 12, accent, true);

        for (int i = 0; i < TABS.length; i++) {
            int ty = py + 36 + i * 22;
            boolean act = i == tab;
            boolean hov = mx >= px + 6 && mx < px + SB - 6 && my >= ty && my < ty + 18;
            if (act) c.fill(px + 6, ty, px + SB - 6, ty + 18, (accent & 0x00FFFFFF) | 0x55000000);
            else if (hov) c.fill(px + 6, ty, px + SB - 6, ty + 18, 0x33FFFFFF);
            c.drawText(textRenderer, TABS[i], px + 14, ty + 5, act ? 0xFFFFFFFF : 0xFF9A9AAA, false);
            final int idx = i;
            hit(px + 6, ty, px + SB - 6, ty + 18, () -> {
                tab = idx;
                scroll = 0;
            });
        }
        c.drawText(textRenderer, "RShift – panel", px + 10, py + H - 26, 0xFF5A5A6A, false);
        c.drawText(textRenderer, "G – oznacz celownik", px + 10, py + H - 15, 0xFF5A5A6A, false);

        c.drawText(textRenderer, "Home › " + TABS[tab], px + SB + 10, py + 12, 0xFF9A9AAA, false);
        search.visible = tab == 0;

        switch (tab) {
            case 0 -> tabPlayers(c, mx, my);
            case 1 -> tabAttack(c, mx, my);
            case 2 -> tabHistory(c);
            case 3 -> tabSettings(c, mx, my);
            default -> tabThemes(c);
        }
        super.render(c, mx, my, delta);
    }

    private void tabPlayers(DrawContext c, int mx, int my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int x0 = px + SB + 10, x1 = px + W - 10, y0 = py + 60, y1 = py + H - 24;
        List<AbstractClientPlayerEntity> list = new ArrayList<>();
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        if (mc.world != null && mc.player != null) {
            for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                if (p != mc.player && p.getName().getString().toLowerCase(Locale.ROOT).contains(q)) list.add(p);
            }
            list.sort(Comparator.comparingDouble(p -> mc.player.distanceTo(p)));
        }
        int maxScroll = Math.max(0, list.size() * ROW - (y1 - y0));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        c.enableScissor(x0, y0, x1, y1);
        clipTop = y0;
        clipBottom = y1;
        for (int i = 0; i < list.size(); i++) {
            int ry = y0 + i * ROW - scroll;
            if (ry + ROW < y0 || ry > y1) continue;
            AbstractClientPlayerEntity p = list.get(i);
            boolean marked = Marks.has(p.getUuid());
            c.fill(x0, ry, x1, ry + ROW - 4, marked ? ((Config.rgb() & 0xFFFFFF) | 0x33000000) : 0xFF16161F);
            if (marked) c.fill(x0, ry, x0 + 2, ry + ROW - 4, Config.argb());
            c.drawText(textRenderer, p.getName().getString(), x0 + 8, ry + 4, 0xFFFFFFFF, true);
            c.drawText(textRenderer, "Dystans: " + (int) mc.player.distanceTo(p) + "m · " + (int) Math.ceil(p.getHealth()) + " HP",
                    x0 + 8, ry + 14, 0xFF9A9AAA, false);
            c.drawText(textRenderer, gear(p), x0 + 8, ry + 24, 0xFF6C8CD5, false);
            button(c, x1 - 70, ry + 6, 62, 18, marked ? "ANULUJ" : "OZNACZ", mx, my, !marked, () -> {
                if (marked) Marks.cancel(p.getUuid());
                else Marks.mark(p);
            });
        }
        clipTop = Integer.MIN_VALUE;
        clipBottom = Integer.MAX_VALUE;
        c.disableScissor();
        c.drawText(textRenderer, "W zasięgu: " + list.size() + " · Oznaczeni: " + Marks.ACTIVE.size(), x0, y1 + 8, 0xFF7A7A8A, false);
    }

    private void tabAttack(DrawContext c, int mx, int my) {
        int x0 = px + SB + 10, x1 = px + W - 10, y = py + 34;
        c.drawText(textRenderer, "Aktywne: " + Marks.ACTIVE.size() + "   ·   W tej sesji: " + Marks.sessionCount, x0, y, 0xFFCCCCDD, false);
        y += 18;
        if (Marks.ACTIVE.isEmpty()) {
            c.drawText(textRenderer, "Brak aktywnych oznaczeń. Oznacz gracza w zakładce Gracze lub klawiszem G.", x0, y, 0xFF7A7A8A, false);
            return;
        }
        for (Marks.Mark m : new ArrayList<>(Marks.ACTIVE.values())) {
            if (y > py + H - 60) break;
            c.fill(x0, y, x1, y + 28, 0xFF16161F);
            c.fill(x0, y, x0 + 2, y + 28, Config.argb());
            c.drawText(textRenderer, m.name, x0 + 8, y + 5, 0xFFFFFFFF, true);
            c.drawText(textRenderer, "Pozostało: " + Marks.fmt(Marks.remaining(m)), x0 + 8, y + 16, 0xFF9A9AAA, false);
            button(c, x1 - 70, y + 5, 62, 18, "ANULUJ", mx, my, false, () -> Marks.cancel(m.id));
            y += 32;
        }
        button(c, x0, py + H - 30, 120, 18, "Anuluj wszystkie", mx, my, false, Marks::cancelAll);
    }

    private void tabHistory(DrawContext c) {
        int x0 = px + SB + 10, y = py + 34;
        if (Marks.HISTORY.isEmpty()) {
            c.drawText(textRenderer, "Historia jest pusta.", x0, y, 0xFF7A7A8A, false);
            return;
        }
        int shown = 0;
        for (int i = Marks.HISTORY.size() - 1; i >= 0 && shown < 17; i--, shown++) {
            c.drawText(textRenderer, Marks.HISTORY.get(i), x0, y, 0xFFCCCCDD, false);
            y += 13;
        }
    }

    private void tabSettings(DrawContext c, int mx, int my) {
        int x0 = px + SB + 10, x1 = px + W - 10, y = py + 34;
        toggle(c, x0, x1, y, "Tracery (linia do celu)", Config.I.tracers, () -> {
            Config.I.tracers = !Config.I.tracers;
            Config.save();
        });
        y += 28;
        toggle(c, x0, x1, y, "Ramka hitboxa celu", Config.I.hitbox, () -> {
            Config.I.hitbox = !Config.I.hitbox;
            Config.save();
        });
        y += 28;
        toggle(c, x0, x1, y, "HUD celu (nick, HP, timer)", Config.I.hud, () -> {
            Config.I.hud = !Config.I.hud;
            Config.save();
        });
        y += 28;
        c.fill(x0, y, x1, y + 24, 0xFF16161F);
        c.drawText(textRenderer, "Czas oznaczenia: " + Config.I.durationSec + " s", x0 + 8, y + 8, 0xFFDDDDE8, false);
        button(c, x1 - 52, y + 3, 20, 18, "-", mx, my, false, () -> {
            Config.I.durationSec = Math.max(10, Config.I.durationSec - 10);
            Config.save();
        });
        button(c, x1 - 28, y + 3, 20, 18, "+", mx, my, false, () -> {
            Config.I.durationSec = Math.min(300, Config.I.durationSec + 10);
            Config.save();
        });
        y += 36;
        c.drawText(textRenderer, "Klawisze zmienisz w: Opcje > Sterowanie > LampaTarget", x0, y, 0xFF7A7A8A, false);
    }

    private void tabThemes(DrawContext c) {
        int x0 = px + SB + 10, x1 = px + W - 10, y0 = py + 34;
        int cw = (x1 - x0 - 16) / 3, ch = 40;
        for (int i = 0; i < Config.NAMES.length; i++) {
            int cx = x0 + (i % 3) * (cw + 8), cy = y0 + (i / 3) * (ch + 8);
            boolean act = i == Config.I.theme;
            int rgb = Config.RGB[i];
            c.fill(cx, cy, cx + cw, cy + ch, act ? ((rgb & 0xFFFFFF) | 0x44000000) : 0xFF16161F);
            c.fill(cx + 8, cy + 10, cx + 28, cy + 30, 0xFF000000 | rgb);
            c.drawText(textRenderer, Config.NAMES[i], cx + 36, cy + 10, 0xFFFFFFFF, false);
            c.drawText(textRenderer, act ? "Aktywny" : "Kliknij, aby wybrać", cx + 36, cy + 22, 0xFF9A9AAA, false);
            final int idx = i;
            hit(cx, cy, cx + cw, cy + ch, () -> {
                Config.I.theme = idx;
                Config.save();
            });
        }
    }

    // ---------- input ----------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Object[] h = hits.get(i);
                if (mx >= (int) h[0] && mx < (int) h[2] && my >= (int) h[1] && my < (int) h[3]) {
                    ((Runnable) h[4]).run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (tab == 0) {
            scroll = Math.max(0, scroll - (int) (vertical * 20));
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }
}
