package com.y271727uy.lumenized.client.shader;

import com.y271727uy.lumenized.LumenizedConstants;
import com.y271727uy.lumenized.comp.iris.IrisHandle;
import com.y271727uy.lumenized.core.MixinPluginShared;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL46;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2022/5/6
 * @implNote RenderUtils
 */
public class RenderUtils {
    public static ShaderInstance blitShader;

    public static void fastBlit(RenderTarget from, RenderTarget to) {
        RenderSystem.assertOnRenderThread();
        if (blitShader == null) {
            LumenizedConstants.LOGGER.warn("Skipping fast blit because the Lumenized shader has not loaded yet");
            return;
        }
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);

        to.bindWrite(true);

        if (MixinPluginShared.IS_IRIS_LOAD && to == Minecraft.getInstance().getMainRenderTarget()) {
            IrisHandle.INSTANCE.bindWriteMain();
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }

        blitShader.setSampler("DiffuseSampler", from.getColorTextureId());

        blitShader.apply();//iris will disable color mask here, open here
        GL46.glColorMaski(0,true, true,true,true);
        GlStateManager._enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferbuilder.addVertex(-1, 1, 0);
        bufferbuilder.addVertex(-1, -1, 0);
        bufferbuilder.addVertex(1, -1, 0);
        bufferbuilder.addVertex(1, 1, 0);
        BufferUploader.draw(bufferbuilder.buildOrThrow());
        blitShader.clear();

        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._enableDepthTest();
    }

    public static PoseStack copyPoseStack(PoseStack poseStack) {
        return poseStack;
    }

    public static Pair<ShaderInstance, Consumer<ShaderInstance>> registerShaders(ResourceManager resourceManager) {
        try {
            return Pair.of(ReloadShaderManager.backupNewShaderInstance(resourceManager, ResourceLocation.fromNamespaceAndPath(LumenizedConstants.MOD_ID, "fast_blit").toString(), DefaultVertexFormat.POSITION), shaderInstance -> {
                blitShader = shaderInstance;
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final boolean DEBUG_LABEL_AVAILABLE = GL.getCapabilities().GL_KHR_debug;

    public static void warpGLDebugLabel(String message, Runnable block) {
        if (DEBUG_LABEL_AVAILABLE && LumenizedConstants.useOpenGlDebugLabel) {
            GL43.glPushDebugGroup(GL43.GL_DEBUG_SOURCE_APPLICATION, 0, message);
            block.run();
            GL43.glPopDebugGroup();
        } else {
            block.run();
        }
    }
}
