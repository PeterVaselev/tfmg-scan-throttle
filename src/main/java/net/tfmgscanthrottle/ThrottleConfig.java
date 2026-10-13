package net.tfmgscanthrottle;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration of the throttle.
 *
 * <p>Deliberately tiny and harmless-looking: one integer, default 600 ticks (30 s).</p>
 */
public final class ThrottleConfig {

    /** Run the scan once every 600 ticks (30 seconds) unless configured otherwise. */
    public static final int DEFAULT_THROTTLE_TICKS = 600;

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue THROTTLE_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        THROTTLE_TICKS = builder
                .comment("How often (in ticks) TFMG scans ticking chunks for empty fluid reservoirs.",
                        "20 ticks = 1 second, so 600 = once every 30 seconds.",
                        "Values <= 1 disable the throttle and restore TFMG's original per-tick scan.",
                        "The scan only removes already-empty reservoirs, so a delay is harmless.")
                .defineInRange("throttleTicks", DEFAULT_THROTTLE_TICKS, 1, 72000);

        SPEC = builder.build();
    }

    private ThrottleConfig() {
    }

    /**
     * @return the configured interval in ticks; falls back to {@link #DEFAULT_THROTTLE_TICKS} when the
     *         config is not loaded yet.
     */
    public static int throttleTicks() {
        try {
            return THROTTLE_TICKS.get();
        } catch (IllegalStateException | NullPointerException e) {
            // Config not loaded (or not registered) yet: behave like the default.
            return DEFAULT_THROTTLE_TICKS;
        }
    }
}
