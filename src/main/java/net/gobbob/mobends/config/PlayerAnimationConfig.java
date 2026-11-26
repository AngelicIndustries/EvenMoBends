package net.gobbob.mobends.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;

/** Client-side switches for each built-in player animation, listed in the menu's player page. */
public final class PlayerAnimationConfig {
    private static final String CATEGORY = "animations.player";

    /** Each switch and its config comment, in the order the config and the menu list them. */
    private static final String[][] SWITCHES = {
        {"stand", "Idle breathing and stance."},
        {"walk", "Walking cycle."},
        {"sneak", "Sneaking pose layered over ground movement."},
        {"sprint", "Sprinting cycle. Falls back to walking when disabled."},
        {"jump", "Ordinary airborne and landing animation."},
        {"falling", "Distinct pose for sustained downward falls."},
        {"flying", "Creative flight and Et Futurum Requiem Elytra flight pose."},
        {"climbing", "Vanilla ladder and vine climbing pose."},
        {"crawling", "Aqua Acrobatics crawling limb cycle. The low pose is retained when disabled."},
        {"swimming", "Surface, idle and Aqua Acrobatics swimming animation."},
        {"diving", "Directional animation used while moving fully underwater."},
        {"bow", "Bow aiming pose."},
        {"eating", "Raise the active hand to eat or drink, including Backhand off-hand use."},
        {"riding", "Mounted player pose."},
        {"rowing", "Et Futurum Requiem paddle-synchronized boating pose. Requires riding; falls back to riding when disabled."},
        {"mining", "Pickaxe and block-mining pose."},
        {"axe", "Axe swing pose."},
        {"attack", "Master switch for sword and unarmed attack animations."},
        {"swordCombo1", "First sword combo swing."},
        {"swordCombo2", "Second sword combo swing."},
        {"swordCombo3", "Third sword combo swing."},
        {"swordStance", "Short recovery stance after a sword swing."},
        {"punch", "Unarmed punch animation."},
        {"punchStance", "Short recovery stance after an unarmed punch."}};

    private static final Map<String, Boolean> ENABLED = new LinkedHashMap<String, Boolean>();

    private PlayerAnimationConfig() {}

    public static void load(Configuration config) {
        config.getCategory(CATEGORY).setLanguageKey("mobends.config.player");
        for (String[] entry : SWITCHES) {
            ENABLED.put(entry[0], config.get(CATEGORY, entry[0], true, entry[1])
                .setLanguageKey("mobends.config.player." + entry[0]).getBoolean(true));
        }
    }

    public static void save(Configuration config) {
        for (String[] entry : SWITCHES) config.get(CATEGORY, entry[0], true).set(isEnabled(entry[0]));
    }

    /** Whether the animation plays; any animation without a switch, or before the config loads, does. */
    public static boolean isEnabled(String animation) {
        Boolean enabled = ENABLED.get(animation);
        return enabled == null || enabled;
    }

    /** Turns an animation on or off at once; {@link #save} writes it. */
    public static void setEnabled(String animation, boolean enabled) {
        ENABLED.put(animation, enabled);
    }

    /** Every switch's name, in order. */
    public static List<String> names() {
        List<String> names = new ArrayList<String>();
        for (String[] entry : SWITCHES) names.add(entry[0]);
        return Collections.unmodifiableList(names);
    }

    /** What a switch's animation is, as its config comment says it. */
    public static String comment(String animation) {
        for (String[] entry : SWITCHES) {
            if (entry[0].equals(animation)) return entry[1];
        }
        return "";
    }
}
