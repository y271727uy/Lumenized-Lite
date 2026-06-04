package com.y271727uy.lumenized.forge;

import com.y271727uy.lumenized.LumenizedConstants;
import com.y271727uy.lumenized.forge.client.ClientProxy;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import org.spongepowered.asm.mixin.Mixins;

@Mod(LumenizedConstants.MOD_ID)
public class LumenizedMod {

    @SuppressWarnings("removal")
    public LumenizedMod() {
        Mixins.addConfiguration("lumenized.mixins.json");
        LumenizedForgeConfig.registerConfig();
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    }

    public static boolean isRubidiumLoaded() {
        return ModList.get().isLoaded("rubidium");
    }
}
