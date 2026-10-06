package com.y271727uy.lumenized.forge.event;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class LumenizedReloadEvent extends Event implements IModBusEvent {
	public final com.y271727uy.lumenized.event.LumenizedReloadEvent event;

	public LumenizedReloadEvent(com.y271727uy.lumenized.event.LumenizedReloadEvent event) {
		this.event = event;
	}
}
