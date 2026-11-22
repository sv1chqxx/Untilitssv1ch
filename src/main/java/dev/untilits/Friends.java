package dev.untilits;

/** Список друзей (зелёные ники). Хранится в конфиге, регистр не важен. */
public final class Friends {
    private Friends() { }

    public static boolean is(String name) {
        if (name == null) return false;
        for (String f : Cfg.D.friends) if (f.equalsIgnoreCase(name)) return true;
        return false;
    }

    public static boolean valid(String n) { return n != null && n.matches("[A-Za-z0-9_]{1,16}"); }

    public static boolean add(String n) {
        if (!valid(n) || is(n)) return false;
        Cfg.D.friends.add(n);
        Cfg.save();
        return true;
    }

    public static boolean remove(String n) {
        boolean r = Cfg.D.friends.removeIf(f -> f.equalsIgnoreCase(n));
        if (r) Cfg.save();
        return r;
    }
}
