package com.y271727uy.lumenized.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class LumenizedMixinConnector implements IMixinConnector {
    @Override
    public void connect() {
        Mixins.addConfiguration("lumenized.mixins.json");
    }
}
