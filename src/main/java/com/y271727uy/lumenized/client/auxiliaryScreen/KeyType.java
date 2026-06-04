package com.y271727uy.lumenized.client.auxiliaryScreen;

import java.util.List;

/**
 * all supported lumenized configurable types
 */
public enum KeyType {

	COLORED_BLOCK, LIGHT_ITEM, BLOOM_PARTICLE, BLOOM_FLUID, BLOOM_BLOCK;
	/**
	 * cache
	 */
	public static final List<KeyType> VALUES = List.of(values());

	public static final KeyType DEFAULT = KeyType.VALUES.get(0);

}
