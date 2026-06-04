package com.y271727uy.lumenized.event;

/**
 * called when lumenized reload colored light and blooms<br>
 * subscribe to do java register<br>
 * for forge: subscriber LumenizedReloadEvent<br>
 * for fabric: use FabricShimmerReloadCallback.EVENT#register
 */
public class LumenizedReloadEvent implements LumenizedEvent {
	public enum ReloadType{
		COLORED_LIGHT,BLOOM
	}

	private ReloadType reloadType;

	public LumenizedReloadEvent(ReloadType reloadType){
		this.reloadType = reloadType;
	}

	public ReloadType getReloadType() {
		return reloadType;
	}
}
