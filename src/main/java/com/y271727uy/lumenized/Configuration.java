package com.y271727uy.lumenized;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.y271727uy.lumenized.config.ColorReferences;
import com.y271727uy.lumenized.config.ColorReferencesTypeAdapter;
import com.y271727uy.lumenized.config.LumenizedConfig;
import com.y271727uy.lumenized.event.LumenizedLoadConfigEvent;
import com.y271727uy.lumenized.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2022/05/05
 * @implNote Configs
 */
public class Configuration {

	private static final String configurationFileName = "lumenized.json";
	/**
	 * config location from mod jar and resource packs
	 */
	private static final ResourceLocation configLocation = ResourceLocation.tryParse(LumenizedConstants.MOD_ID + ":" + configurationFileName);

	/**
	 * the Gson object, with pretty print
	 */
	public static final Gson gson = new GsonBuilder()
			.registerTypeAdapter(ColorReferences.class,new ColorReferencesTypeAdapter())
			.setPrettyPrinting()
			.create();
	/**
	 * the collection stores all the configs from file and mod
	 */
	public static final List<LumenizedConfig> configs = new ArrayList<>();
	/**
	 * the config object from auxiliary screen
	 */
	@Nullable public static LumenizedConfig auxiliaryConfig;

	/**
	 * load all configs
	 */
	public static void load() {
		configs.clear();
		String causedSource = "unknown";
		try {
			//from mod jar and resource pack
			List<Resource> resources = Minecraft.getInstance().getResourceManager().getResourceStack(configLocation);
			for (var resource : resources) {
				causedSource = " file managed my minecraft located in" + " [sourceName:" + resource.sourcePackId() + "," + "location:" + resource.sourcePackId() + "]";
				try (InputStreamReader reader = new InputStreamReader(resource.open())) {
					LumenizedConfig config = gson.fromJson(reader, LumenizedConfig.class);
					if (config.check(causedSource)) {
						if (config.buildIn.get()) {
							if (Services.PLATFORM.enableBuildinSetting()) {
								LumenizedConstants.LOGGER.info("buildIn lumenized configuration is enabled, this can be disabled by config file");
								causedSource = " buildIn lumenized configuration";
							} else {
								LumenizedConstants.LOGGER.info("buildIn lumenized configuration has been disabled by config file");
								continue;
							}
						} else  {
							LumenizedConstants.LOGGER.info("mod jar and resource pack discovery:" + causedSource);
						}
						configs.add(config);
					}
				}
			}
			//automatic mod compat discovery
			for (var modId : Services.PLATFORM.getLoadedMods()) {
				if (modId.equals(LumenizedConstants.MOD_ID)) continue;
				causedSource = " automatic configuration added by mod " + modId;
				ResourceLocation candidateConfigurationPath = ResourceLocation.tryParse(modId + ":" + configurationFileName);
				if (candidateConfigurationPath == null) continue;
				Optional<String> optionalConfiguration = readConfiguration(candidateConfigurationPath);
				if (optionalConfiguration.isPresent()) {
					LumenizedConfig config = gson.fromJson(optionalConfiguration.get(), LumenizedConfig.class);
					if (config.check(causedSource)) {
						configs.add(config);
						LumenizedConstants.LOGGER.info("automatic configuration added by mod:" + modId + " path:" + candidateConfigurationPath);
					}
				}
			}
			//added by mods through event
			for (var entry : Services.PLATFORM.postLoadConfigurationEvent(new LumenizedLoadConfigEvent()).getConfiguration().entrySet()) {
				causedSource = " configuration added by mod " + entry.getKey();
				LumenizedConfig config = gson.fromJson(entry.getValue(), LumenizedConfig.class);
				if (config.check(causedSource)) configs.add(config);
			}

			//from config file
			File shimmerConfigDir = Services.PLATFORM.getConfigDir().resolve("lumenized").toFile();
			if (!shimmerConfigDir.exists() || !shimmerConfigDir.isDirectory()) shimmerConfigDir.mkdir();
			var configFiles = Objects.requireNonNullElse(shimmerConfigDir.listFiles(),new File[0]);
			for (var configFile : configFiles) {
				if (!configFile.getName().endsWith(".json")) return;
				causedSource = " file in config folder:" + configFile.getAbsolutePath();
				if (configFile.isDirectory()) continue;
				try (var stream = new FileReader(configFile)) {
					LumenizedConfig config = gson.fromJson(stream, LumenizedConfig.class);
					if (config.check(causedSource)){
						if (config.enable.get()){
							configs.add(config);
						}else {
							LumenizedConstants.LOGGER.info("skip disabled config file from" + causedSource);
						}
					}
				}
			}

			//from auxiliary screen
			if (auxiliaryConfig != null) {
				LumenizedConstants.LOGGER.info("configurations made by auxiliary screen added");
				configs.add(auxiliaryConfig);
			}

		} catch (IOException ioException) {
			LumenizedConstants.LOGGER.error("failed to get config resources, caused by " + causedSource);
			LumenizedConstants.LOGGER.error(ioException.getMessage());
		} catch (JsonSyntaxException jsonSyntaxException) {
			LumenizedConstants.LOGGER.error("json syntax error in " + causedSource);
			LumenizedConstants.LOGGER.error(jsonSyntaxException.getMessage());
		} catch (SecurityException e){
			LumenizedConstants.LOGGER.error("has no permission to create lumenized config directory");
			LumenizedConstants.LOGGER.error(e.getMessage());
		} catch (Exception e) {
			LumenizedConstants.LOGGER.error("an un-expected exception happen while reloading config files, caused by" + causedSource);
			LumenizedConstants.LOGGER.error(e.getMessage());
		} finally {
			LumenizedConstants.LOGGER.debug("reloading config files end");
		}
	}

	/**
	 * read lumenized configuration from path
	 */
	public static Optional<String> readConfiguration(ResourceLocation configurationPath) {
		var resources = Minecraft.getInstance().getResourceManager().getResourceStack(configurationPath);
		if (resources.size() > 1) {
			LumenizedConstants.LOGGER.error("find multi lumenized configuration file under " + configurationPath);
			LumenizedConstants.LOGGER.error("will only load the first");
			for (int i = 0; i < resources.size(); i++) {
				var res = resources.get(i);
				LumenizedConstants.LOGGER.error("index:{}, sourcePackName{}",i,res);
			}
		}
		return resources.stream().findFirst().flatMap(resource -> {
			try (BufferedReader reader = resource.openAsReader()) {
				return Optional.of(reader.lines().collect(Collectors.joining()));
			} catch (IOException ioException) {
				LumenizedConstants.LOGGER.error("find lumenized configuration file:" + configurationPath + " but failed to read", ioException);
			}
			return Optional.empty();
		});
	}
}
