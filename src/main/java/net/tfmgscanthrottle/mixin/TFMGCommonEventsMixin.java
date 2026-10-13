package net.tfmgscanthrottle.mixin;

import com.drmangotea.tfmg.base.events.TFMGCommonEvents;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.tfmgscanthrottle.ThrottleState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles TFMG's per-tick sweep over all ticking chunks.
 *
 * <p>{@code TFMGCommonEvents.levelTick(LevelTickEvent.Post)} walks
 * {@code ChunkMapAccessor.getVisibleChunkMap()} on every tick of every dimension and, for each
 * ticking chunk, checks whether it carries a {@code FluidReservoir} that is already empty in order
 * to drop it. On a server with a couple of thousand ticking chunks and no TFMG machinery at all,
 * that sweep alone consumed ~12% of the tick budget (spark profile).</p>
 *
 * <p>The cleanup itself is only a garbage collector for empty reservoirs: an empty reservoir is
 * removed either way, the only difference is how long it lingers. So instead of letting the sweep
 * run every tick, the injected handler cancels the call outright and lets it through only once
 * every N ticks (default 600 = 30 s, configurable). No other part of TFMG is touched.</p>
 */
@Mixin(value = TFMGCommonEvents.class, remap = false)
public abstract class TFMGCommonEventsMixin {

    @Inject(
            method = "levelTick(Lnet/neoforged/neoforge/event/tick/LevelTickEvent$Post;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void tfmgscanthrottle$throttleReservoirScan(LevelTickEvent.Post event, CallbackInfo ci) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            // Client levels never ran the sweep, so there is nothing to throttle.
            return;
        }

        if (!ThrottleState.shouldRunScan(level)) {
            // Skip this tick's sweep: TFMG's method body is nothing but the chunk loop.
            ci.cancel();
        }
    }
}
