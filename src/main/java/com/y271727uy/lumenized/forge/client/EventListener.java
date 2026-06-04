package com.y271727uy.lumenized.forge.client;

import com.y271727uy.lumenized.LumenizedConstants;
import com.y271727uy.lumenized.Utils;
import com.y271727uy.lumenized.client.auxiliaryScreen.AuxiliaryScreen;
import com.y271727uy.lumenized.client.auxiliaryScreen.Eyedropper;
import com.y271727uy.lumenized.client.light.ItemEntityLightSourceManager;
import com.y271727uy.lumenized.client.light.LightCounter;
import com.y271727uy.lumenized.client.light.LightManager;
import com.y271727uy.lumenized.client.postprocessing.PostProcessing;
import com.y271727uy.lumenized.client.shader.ReloadShaderManager;
import com.y271727uy.lumenized.forge.LumenizedForgeConfig;
import com.y271727uy.lumenized.platform.Services;
import com.y271727uy.lumenized.renderdoc.RenderDoc;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraft.commands.Commands.literal;

/**
 * @author KilaBash
 * @date 2022/5/12
 * @implNote EventListener
 */
@Mod.EventBusSubscriber(modid = LumenizedConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class EventListener {

    @SubscribeEvent
    public static void onClientTickPost(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ItemEntityLightSourceManager.onAllItemEntityTickEnd();
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() == Minecraft.getInstance().level) {
            LightManager.clear();
        }
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("lumenized")
                .then(Commands.literal("reload_postprocessing")
                        .executes(context -> {
                            for (PostProcessing post : PostProcessing.values()) {
                                post.onResourceManagerReload(null);
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("clear_lights")
                        .executes(context -> {
                            LightManager.clear();
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("reload_shader")
                        .executes(context -> {
                            ReloadShaderManager.reloadShader();
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("confirm_clear_resource")
                        .executes(context -> {
                            ReloadShaderManager.cleanResource();
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.literal("colored_light")
                        .then(Commands.argument("switch_state", BoolArgumentType.bool()).executes(
                                context -> {
                                    LumenizedForgeConfig.getColoredLightEnable().set(context.getArgument("switch_state", Boolean.class));
                                    return Command.SINGLE_SUCCESS;
                                }
                        )))
                .then(Commands.literal("bloom")
                        .then(Commands.argument("switch_state", BoolArgumentType.bool()).executes(
                                context -> {
                                    LumenizedForgeConfig.getBloomEnable().set(context.getArgument("switch_state", Boolean.class));
                                    return Command.SINGLE_SUCCESS;
                                }
                        )))
                .then(Commands.literal("additive_blend")
                        .then(Commands.argument("switch_state", BoolArgumentType.bool()).executes(
                                context -> {
                                    LumenizedForgeConfig.getAdditiveBlend().set(context.getArgument("switch_state", Boolean.class));
                                    ReloadShaderManager.reloadShader();
                                    return Command.SINGLE_SUCCESS;
                                }
                        )))
                .then(Commands.literal("auxiliary_screen")
                        .executes(context -> {
                            Minecraft.getInstance().tell(()-> Minecraft.getInstance().setScreen(new AuxiliaryScreen()));
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("eyedropper")
                        .executes(context -> {
                            if (Eyedropper.getState()) {
                                context.getSource().sendSystemMessage(Component.literal("exit eyedropper mode"));
                            } else {
                                context.getSource().sendSystemMessage(Component.literal("enter eyedropper mode, backend: " + Eyedropper.mode.modeName()));
                            }
                            Eyedropper.switchState();
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("eyedropper").then(literal("backend")
                        .then(literal("ShaderStorageBufferObject").executes(
                                context -> {
                                    Eyedropper.switchMode(Eyedropper.ShaderStorageBufferObject);
                                    return Command.SINGLE_SUCCESS;
                                }
                        )).then(literal("glGetTexImage").executes(
                                context -> {
                                    Eyedropper.switchMode(Eyedropper.DOWNLOAD);
                                    return Command.SINGLE_SUCCESS;
                                }
                        ))))
                .then(Commands.literal("dumpLightBlockStates")
                        .executes(context -> {
                            if (Utils.dumpAllLightingBlocks()){
                                context.getSource().sendSuccess(() -> Component.literal("dump successfully to cfg/lumenized/LightBlocks.txt"),false);
                            }else {
                                context.getSource().sendFailure(Component.literal("dump failed, see log for detailed information"));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("renderDoc")
                        .executes(context -> {
                            if (Services.PLATFORM.isRenderDocEnable()) {
                                var pid = RenderDoc.launchReplayUI(true);
                                if (pid == 0) {
                                    context.getSource().sendFailure(Component.literal("unable to init renderDoc"));
                                } else {
                                    context.getSource().sendSuccess(() -> Component.literal("openSuccess, pid=" + pid),true);
                                }
                            } else {
                                context.getSource().sendFailure(Component.literal("renderDoc not enable"));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("coloredLightMonitor")
                        .executes(context -> {
                            LightCounter.Render.enable = !LightCounter.Render.enable;
                            context.getSource().sendSystemMessage(Component.literal("switch monitor to " +
                                    (LightCounter.Render.enable ? "enable" : "disable")));
                            return Command.SINGLE_SUCCESS;
                        }))
        );
    }
}
