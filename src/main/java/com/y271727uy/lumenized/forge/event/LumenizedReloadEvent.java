package com.y271727uy.lumenized.forge.event;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

public class LumenizedReloadEvent extends Event implements IModBusEvent {
	public final com.y271727uy.lumenized.event.LumenizedReloadEvent event;

	public LumenizedReloadEvent(com.y271727uy.lumenized.event.LumenizedReloadEvent event) {
		this.event = event;
	}
}
