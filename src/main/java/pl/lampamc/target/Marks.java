package pl.lampamc.target;

import net.minecraft.entity.player.PlayerEntity;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Marks {
    public static class Mark {
        public final UUID id;
        public final String name;
        public final long start;
        public final long dur;

        Mark(UUID id, String name, long start, long dur) {
            this.id = id;
            this.name = name;
            this.start = start;
            this.dur = dur;
        }
    }

    public static final Map<UUID, Mark> ACTIVE = new LinkedHashMap<>();
    public static final List<String> HISTORY = new ArrayList<>();
    public static int sessionCount = 0;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static void log(String s) {
        HISTORY.add("[" + LocalTime.now().format(TIME) + "] " + s);
        if (HISTORY.size() > 200) HISTORY.remove(0);
    }

    public static void mark(PlayerEntity p) {
        String name = p.getName().getString();
        ACTIVE.put(p.getUuid(), new Mark(p.getUuid(), name, System.currentTimeMillis(), Config.I.durationSec * 1000L));
        sessionCount++;
        log(name + " – oznaczony (" + Config.I.durationSec + " s)");
    }

    public static void cancel(UUID id) {
        Mark m = ACTIVE.remove(id);
        if (m != null) log(m.name + " – anulowano");
    }

    public static void cancelAll() {
        for (Mark m : new ArrayList<>(ACTIVE.values())) cancel(m.id);
    }

    public static boolean has(UUID id) {
        return ACTIVE.containsKey(id);
    }

    public static long remaining(Mark m) {
        return m.start + m.dur - System.currentTimeMillis();
    }

    public static String fmt(long ms) {
        long s = Math.max(0, ms / 1000);
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    public static void tick() {
        Iterator<Mark> it = ACTIVE.values().iterator();
        while (it.hasNext()) {
            Mark m = it.next();
            if (remaining(m) <= 0) {
                it.remove();
                log(m.name + " – czas minął");
            }
        }
    }
}
