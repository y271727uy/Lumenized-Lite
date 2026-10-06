package com.y271727uy.lumenized.forge;

import com.y271727uy.lumenized.LumenizedConstants;
import com.y271727uy.lumenized.forge.client.ClientProxy;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import org.spongepowered.asm.mixin.Mixins;

@Mod(LumenizedConstants.MOD_ID)
public class LumenizedMod {

    public LumenizedMod() {
        Mixins.addConfiguration("lumenized.mixins.json");
        LumenizedForgeConfig.registerConfig();
        if (FMLLoader.getDist().isClient()) {
            new ClientProxy();
        }
        else new CommonProxy();
    }

    public static boolean isSodiumLoaded() {
        return ModList.get().isLoaded("sodium");
    }
}
