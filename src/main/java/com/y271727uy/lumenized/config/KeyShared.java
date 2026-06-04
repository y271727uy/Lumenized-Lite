package com.y271727uy.lumenized.config;

public abstract class KeyShared implements Check {
	LumenizedConfig lumenizedConfig;

	@Override
	public String getConfigSource() {
		return lumenizedConfig.configSource;
	}
}