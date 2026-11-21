package dev.untilits;

/** Оценка TPS сервера по интервалу между пакетами обновления времени (раз в 20 тиков). */
public final class Tps {
    private static final float[] S = new float[5];
    private static int n = 0, idx = 0;
    private static long last = 0;

    private Tps() { }

    public static void onTimePacket() {
        long now = System.nanoTime();
        if (last != 0) {
            double dt = (now - last) / 1e9;
            if (dt > 10) { n = 0; }
            else if (dt > 0.2) {
                S[idx] = (float) Math.min(20.0, 20.0 / dt);
                idx = (idx + 1) % S.length;
                n = Math.min(n + 1, S.length);
            }
        }
        last = now;
    }

    public static float get() {
        if (n == 0) return 20f;
        float s = 0;
        for (int i = 0; i < n; i++) s += S[i];
        return Math.round(s / n * 10f) / 10f;
    }
}
