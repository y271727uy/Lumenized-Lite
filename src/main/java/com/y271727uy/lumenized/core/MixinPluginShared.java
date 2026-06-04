package com.y271727uy.lumenized.core;

import com.y271727uy.lumenized.LumenizedConstants;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public interface MixinPluginShared {

	static boolean isClassFound(String className) {
		try {
			Class.forName(className, false, Thread.currentThread().getContextClassLoader());
			LumenizedConstants.LOGGER.debug("find class {}", className);
			return true;
		} catch (ClassNotFoundException e) {
			LumenizedConstants.LOGGER.debug("can't find class {}", className);
			return false;
		}
	}

	private static boolean checkOptifine() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			if (Objects.equals(classLoader.getName(),"TRANSFORMER")) {
				try {
					var fmlLoaderClass = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
					var getGameLayerMethod = fmlLoaderClass.getMethod("getGameLayer");
					var gameLayer = getGameLayerMethod.invoke(null);
					var configurationMethod = gameLayer.getClass().getMethod("configuration");
					var configuration = (java.lang.module.Configuration)configurationMethod.invoke(gameLayer);
					return configuration.toString().contains("optifine");
				} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
					LumenizedConstants.LOGGER.catching(e);
				}
				return isClassFound("optifine.Installer");
			}
		} catch (Exception e){
			LumenizedConstants.LOGGER.catching(e);
		}
		return false;
	}

	private static boolean doUnderOptifine(boolean underOptifine) {
		if (underOptifine) {
			LumenizedConstants.LOGGER.error("detect lumenized is running under optifine, all the functions are disabled, consider just remove lumenized");
		}
		return underOptifine;
	}

	boolean IS_OPT_LOAD = doUnderOptifine(isClassFound("optifine.OptiFineTranformationService") || checkOptifine());
	boolean IS_DASH_LOADER = isClassFound("dev.quantumfusion.dashloader.mixin.MixinPlugin");

	boolean IS_SODIUM_LOAD = isClassFound("me.jellysquid.mods.sodium.mixin.SodiumMixinPlugin");
	boolean IS_RUBIDIUM_LOAD = IS_SODIUM_LOAD;

	boolean IS_IRIS_LOAD = isClassFound("net.coderbot.iris.compat.sodium.mixin.IrisSodiumCompatMixinPlugin");
	boolean IS_OCULUS_LOAD = IS_IRIS_LOAD;

}
