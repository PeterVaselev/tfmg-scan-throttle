package net.tfmgscanthrottle;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the TFMG Scan Throttle companion mod.
 *
 * <p>The actual work lives in {@code net.tfmgscanthrottle.mixin.TFMGCommonEventsMixin}, which
 * rate-limits TFMG's per-tick sweep over every ticking chunk that looks for empty fluid reservoirs.</p>
 */
@Mod(TfmgScanThrottle.MOD_ID)
public final class TfmgScanThrottle {

    public static final String MOD_ID = "tfmgscanthrottle";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public TfmgScanThrottle(IEventBus modEventBus, ModContainer modContainer) {
        // COMMON config so the value is read at startup, on both sides, into a plain
        // config/tfmgscanthrottle-common.toml. File name is fixed by NeoForge: <modid>-common.toml.
        modContainer.registerConfig(ModConfig.Type.COMMON, ThrottleConfig.SPEC);

        // Print the effective interval once the file is actually loaded, not during construction
        // (config values are not readable before the loading event fires).
        modEventBus.addListener(ModConfigEvent.Loading.class, TfmgScanThrottle::logEffectiveInterval);
        modEventBus.addListener(ModConfigEvent.Reloading.class, TfmgScanThrottle::logEffectiveInterval);

        LOGGER.info("TFMG scan throttle enabled; interval comes from config/tfmgscanthrottle-common.toml"
                + " (default: {} ticks)", ThrottleConfig.DEFAULT_THROTTLE_TICKS);
    }

    private static void logEffectiveInterval(ModConfigEvent event) {
        if (event.getConfig().getSpec() == ThrottleConfig.SPEC) {
            LOGGER.info("TFMG scan throttle: chunk scan interval = {} ticks", ThrottleConfig.throttleTicks());
        }
    }
}
