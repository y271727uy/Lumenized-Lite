package com.y271727uy.lumenized.config;

import com.y271727uy.lumenized.LumenizedConstants;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import java.util.Objects;

interface FluidChecker extends Check {

	String getFluidName();

	default Pair<ResourceLocation, Fluid> fluid() {
		var fluidName = getFluidName();
		Objects.requireNonNull(fluidName);
		if (!ResourceLocation.isValidResourceLocation(fluidName)) {
			LumenizedConstants.LOGGER.error("invalid fluid name " + fluidName + " form" + getConfigSource());
			return null;
		}
		var fluidLocation = Objects.requireNonNull(ResourceLocation.tryParse(fluidName));
		if (!BuiltInRegistries.FLUID.containsKey(fluidLocation)) {
			LumenizedConstants.LOGGER.error("can't find fluid " + fluidLocation + " from" + getConfigSource());
			return Pair.of(fluidLocation, null);
		}
		Fluid fluid = BuiltInRegistries.FLUID.get(fluidLocation);
		return Pair.of(fluidLocation, fluid);
	}
}
