package com.y271727uy.lumenized.forge.event;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;


public class LumenizedLoadConfigEvent extends Event implements IModBusEvent {
    public final com.y271727uy.lumenized.event.LumenizedLoadConfigEvent event;

    public LumenizedLoadConfigEvent(com.y271727uy.lumenized.event.LumenizedLoadConfigEvent event) {
        this.event = event;
    }

}
