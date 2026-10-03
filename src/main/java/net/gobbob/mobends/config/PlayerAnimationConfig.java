package net.gobbob.mobends.config;

import net.minecraftforge.common.config.Configuration;

/** Client-side switches for each built-in player animation. */
public final class PlayerAnimationConfig {
    private static final String CATEGORY = "animations.player";

    private static boolean stand = true;
    private static boolean walk = true;
    private static boolean sneak = true;
    private static boolean sprint = true;
    private static boolean jump = true;
    private static boolean falling = true;
    private static boolean flying = true;
    private static boolean climbing = true;
    private static boolean crawling = true;
    private static boolean swimming = true;
    private static boolean diving = true;
    private static boolean bow = true;
    private static boolean eating = true;
    private static boolean riding = true;
    private static boolean rowing = true;
    private static boolean mining = true;
    private static boolean axe = true;
    private static boolean attack = true;
    private static boolean swordCombo1 = true;
    private static boolean swordCombo2 = true;
    private static boolean swordCombo3 = true;
    private static boolean swordStance = true;
    private static boolean punch = true;
    private static boolean punchStance = true;

    private PlayerAnimationConfig() {}

    public static void load(Configuration config) {
        stand = read(config, "stand", "Idle breathing and stance.");
        walk = read(config, "walk", "Walking cycle.");
        sneak = read(config, "sneak", "Sneaking pose layered over ground movement.");
        sprint = read(config, "sprint", "Sprinting cycle. Falls back to walking when disabled.");
        jump = read(config, "jump", "Ordinary airborne and landing animation.");
        falling = read(config, "falling", "Distinct pose for sustained downward falls.");
        flying = read(config, "flying", "Creative flight and Et Futurum Requiem Elytra flight pose.");
        climbing = read(config, "climbing", "Vanilla ladder and vine climbing pose.");
        crawling = read(config, "crawling", "Aqua Acrobatics crawling limb cycle. The low pose is retained when disabled.");
        swimming = read(config, "swimming", "Surface, idle and Aqua Acrobatics swimming animation.");
        diving = read(config, "diving", "Directional animation used while moving fully underwater.");
        bow = read(config, "bow", "Bow aiming pose.");
        eating = read(config, "eating", "Raise the active hand to eat or drink, including Backhand off-hand use.");
        riding = read(config, "riding", "Mounted player pose.");
        rowing = read(config, "rowing", "Et Futurum Requiem paddle-synchronized boating pose. Requires riding; falls back to riding when disabled.");
        mining = read(config, "mining", "Pickaxe and block-mining pose.");
        axe = read(config, "axe", "Axe swing pose.");
        attack = read(config, "attack", "Master switch for sword and unarmed attack animations.");
        swordCombo1 = read(config, "swordCombo1", "First sword combo swing.");
        swordCombo2 = read(config, "swordCombo2", "Second sword combo swing.");
        swordCombo3 = read(config, "swordCombo3", "Third sword combo swing.");
        swordStance = read(config, "swordStance", "Short recovery stance after a sword swing.");
        punch = read(config, "punch", "Unarmed punch animation.");
        punchStance = read(config, "punchStance", "Short recovery stance after an unarmed punch.");
    }

    public static void save(Configuration config) {
        write(config, "stand", stand);
        write(config, "walk", walk);
        write(config, "sneak", sneak);
        write(config, "sprint", sprint);
        write(config, "jump", jump);
        write(config, "falling", falling);
        write(config, "flying", flying);
        write(config, "climbing", climbing);
        write(config, "crawling", crawling);
        write(config, "swimming", swimming);
        write(config, "diving", diving);
        write(config, "bow", bow);
        write(config, "eating", eating);
        write(config, "riding", riding);
        write(config, "rowing", rowing);
        write(config, "mining", mining);
        write(config, "axe", axe);
        write(config, "attack", attack);
        write(config, "swordCombo1", swordCombo1);
        write(config, "swordCombo2", swordCombo2);
        write(config, "swordCombo3", swordCombo3);
        write(config, "swordStance", swordStance);
        write(config, "punch", punch);
        write(config, "punchStance", punchStance);
    }

    public static boolean isEnabled(String animation) {
        if ("stand".equals(animation)) return stand;
        if ("walk".equals(animation)) return walk;
        if ("sneak".equals(animation)) return sneak;
        if ("sprint".equals(animation)) return sprint;
        if ("jump".equals(animation)) return jump;
        if ("falling".equals(animation)) return falling;
        if ("flying".equals(animation)) return flying;
        if ("climbing".equals(animation)) return climbing;
        if ("crawling".equals(animation)) return crawling;
        if ("swimming".equals(animation)) return swimming;
        if ("diving".equals(animation)) return diving;
        if ("bow".equals(animation)) return bow;
        if ("eating".equals(animation)) return eating;
        if ("riding".equals(animation)) return riding;
        if ("rowing".equals(animation)) return rowing;
        if ("mining".equals(animation)) return mining;
        if ("axe".equals(animation)) return axe;
        if ("attack".equals(animation)) return attack;
        if ("swordCombo1".equals(animation)) return swordCombo1;
        if ("swordCombo2".equals(animation)) return swordCombo2;
        if ("swordCombo3".equals(animation)) return swordCombo3;
        if ("swordStance".equals(animation)) return swordStance;
        if ("punch".equals(animation)) return punch;
        if ("punchStance".equals(animation)) return punchStance;
        return true;
    }

    private static boolean read(Configuration config, String name, String comment) {
        return config.get(CATEGORY, name, true, comment).getBoolean(true);
    }

    private static void write(Configuration config, String name, boolean value) {
        config.get(CATEGORY, name, true).set(value);
    }
}
