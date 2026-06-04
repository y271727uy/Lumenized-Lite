package com.y271727uy.lumenized.client.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.y271727uy.lumenized.LumenizedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record LumenizedMetadataSection(boolean bloom) {
    public static final String SECTION_NAME = LumenizedConstants.MOD_ID;
    private static final Map<ResourceLocation, LumenizedMetadataSection> METADATA_CACHE = new ConcurrentHashMap<>();
    public static final LumenizedMetadataSection MISSING = new LumenizedMetadataSection(false);

    public static void clearCache() {
        METADATA_CACHE.clear();
    }

    @Nullable
    public static LumenizedMetadataSection getMetadata(ResourceLocation res) {
        if (METADATA_CACHE.containsKey(res)) {
            return METADATA_CACHE.get(res);
        }
        LumenizedMetadataSection ret = MISSING;
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(res);
            if (resource.isPresent()) {
                ret = resource.get().metadata().getSection(Serializer.INSTANCE).orElse(MISSING);
            }
        } catch (Throwable ignored){
        }
        METADATA_CACHE.put(res, ret);
        return ret;
    }

    public static boolean isBloom(TextureAtlasSprite sprite) {
        if (sprite == null) return false;
        @SuppressWarnings("resource")
        LumenizedMetadataSection ret = getMetadata(spriteToAbsolute(sprite.contents().name()));
        return ret != null && ret.bloom;
    }

    public static ResourceLocation spriteToAbsolute(ResourceLocation sprite) {
        if (!sprite.getPath().startsWith("textures/")) {
            sprite = new ResourceLocation(sprite.getNamespace(), "textures/" + sprite.getPath());
        }
        if (!sprite.getPath().endsWith(".png")) {
            sprite = new ResourceLocation(sprite.getNamespace(), sprite.getPath() + ".png");
        }
        return sprite;
    }

    public static void onResourceManagerReload() {
        METADATA_CACHE.clear();
    }

    public static class Serializer
            implements MetadataSectionSerializer<LumenizedMetadataSection> {
        static Serializer INSTANCE = new Serializer();

        @Override
        @NotNull
        public String getMetadataSectionName() {
            return SECTION_NAME;
        }

        @Override
        @NotNull
        public LumenizedMetadataSection fromJson(@NotNull JsonObject json) {
            boolean bloom = false;
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("bloom")) {
                    JsonElement element = obj.get("bloom");
                    if (element.isJsonPrimitive() &&
                            element.getAsJsonPrimitive().isBoolean()) {
                        bloom = element.getAsBoolean();
                    }
                }
            }
            return new LumenizedMetadataSection(bloom);
        }
    }
}
