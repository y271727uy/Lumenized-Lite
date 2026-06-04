package com.y271727uy.lumenized.forge.event;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;


public class LumenizedLoadConfigEvent extends Event implements IModBusEvent {
    public final com.y271727uy.lumenized.event.LumenizedLoadConfigEvent event;

    public LumenizedLoadConfigEvent(com.y271727uy.lumenized.event.LumenizedLoadConfigEvent event) {
        this.event = event;
    }

}
