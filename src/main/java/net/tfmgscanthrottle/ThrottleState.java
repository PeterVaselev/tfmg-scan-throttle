package net.tfmgscanthrottle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Per-dimension tick counters used to decide whether TFMG's chunk sweep should run this tick.
 *
 * <p>Keyed by dimension instead of by {@link ServerLevel} instance so a world reload cannot leak
 * level objects; the map holds at most one entry per dimension.</p>
 */
public final class ThrottleState {

    private static final Map<ResourceKey<Level>, long[]> TICK_COUNTERS = new ConcurrentHashMap<>();

    private ThrottleState() {
    }

    /**
     * @return true when the caller should let TFMG's scan run this tick.
     */
    public static boolean shouldRunScan(ServerLevel level) {
        int throttle = ThrottleConfig.throttleTicks();
        if (throttle <= 1) {
            // Throttle disabled: keep TFMG's original behaviour.
            return true;
        }

        long[] counter = TICK_COUNTERS.computeIfAbsent(level.dimension(), key -> new long[1]);
        long tick = counter[0]++;
        return tick % throttle == 0;
    }
}
