package com.y271727uy.lumenized.platform;

import com.y271727uy.lumenized.LumenizedConstants;
import com.y271727uy.lumenized.platform.services.IPlatformHelper;
import net.minecraft.client.ClientBrandRetriever;

/**
 * @author HypherionSA
 * @date 2022/06/09
 */
public class Services {

    public static final IPlatformHelper PLATFORM = load();

    private static IPlatformHelper load() {
        String loaderName = ClientBrandRetriever.getClientModName().toLowerCase().trim();
        var classLocation = switch (loaderName) {
            case "forge" -> "com.y271727uy.lumenized.forge.platform.ForgePlatformHelper";
            case "fabric" -> "com.y271727uy.lumenized.fabric.platform.FabricPlatformHelper";
            case "quilt" -> {
                LumenizedConstants.LOGGER.warn("quilt detected, just work under fabric");
                LumenizedConstants.LOGGER.warn("behaviour may ne be correct");
                yield "com.y271727uy.lumenized.fabric.platform.FabricPlatformHelper";
            }
            //https://github.com/sp614x/optifine/issues/1631
            case "optifine" -> throw new RuntimeException("doesn't support under optifine, please uninstall lumenized");
            case "vanilla" -> throw new RuntimeException("run on vanilla?");
            default -> throw new RuntimeException("unknown loader " + loaderName);
        };
        LumenizedConstants.LOGGER.debug("detect loader " + loaderName);
        IPlatformHelper loadedService;
        try {
            loadedService = (IPlatformHelper) Class.forName(classLocation).getConstructor().newInstance();
        } catch (Exception e) {
            LumenizedConstants.LOGGER.error("failed to init PlatformHelper");
            throw new RuntimeException(e);
        }
        LumenizedConstants.LOGGER.debug("Loaded {} for service", loadedService);
        return loadedService;
    }
}
