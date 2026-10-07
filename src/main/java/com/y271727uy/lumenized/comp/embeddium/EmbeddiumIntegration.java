package com.y271727uy.lumenized.comp.embeddium;

import com.y271727uy.lumenized.forge.LumenizedForgeConfig;
import com.y271727uy.lumenized.platform.Services;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;

public final class EmbeddiumIntegration {
    private static final OptionStorage<Void> STORAGE = new OptionStorage<>() {
        @Override
        public Void getData() {
            return null;
        }

        @Override
        public void save() {
        }
    };

    private EmbeddiumIntegration() {
    }

    public static void register() {
        OptionGUIConstructionEvent.BUS.addListener(event -> event.addPage(createPage()));
    }

    private static OptionPage createPage() {
        OptionGroup.Builder group = OptionGroup.createBuilder();
        group.add(booleanOption("enable_post_processing", "lumenized.config.enable_post_processing",
                LumenizedForgeConfig.getEnablePostProcessing()::get,
                LumenizedForgeConfig.getEnablePostProcessing()::set));
        group.add(booleanOption("enable_colored_light", "lumenized.config.enable_colored_light",
                LumenizedForgeConfig.getColoredLightEnable()::get,
                LumenizedForgeConfig.getColoredLightEnable()::set));
        group.add(booleanOption("enable_bloom", "lumenized.config.enable_bloom",
                LumenizedForgeConfig.getBloomEnable()::get,
                LumenizedForgeConfig.getBloomEnable()::set));
        group.add(booleanOption("block_bloom", "lumenized.config.block_bloom",
                LumenizedForgeConfig.getBlockBloom()::get,
                LumenizedForgeConfig.getBlockBloom()::set));
        group.add(booleanOption("light_map", "lumenized.config.light_map",
                LumenizedForgeConfig.getLightMap()::get,
                LumenizedForgeConfig.getLightMap()::set));
        group.add(booleanOption("additive_blend", "lumenized.config.additive_blend",
                LumenizedForgeConfig.getAdditiveBlend()::get,
                LumenizedForgeConfig.getAdditiveBlend()::set));
        group.add(booleanOption("builtin_setting", "lumenized.config.builtin_setting",
                LumenizedForgeConfig.getEnableBuildinSetting()::get,
                LumenizedForgeConfig.getEnableBuildinSetting()::set));
        group.add(integerOption("bloom_color_attachment", "lumenized.config.bloom_color_attachment",
                () -> LumenizedForgeConfig.getBloomColorAttachmentNumber().get(),
                value -> LumenizedForgeConfig.getBloomColorAttachmentNumber().set(value), 1, 16));
        group.add(booleanOption("inset_shader", "lumenized.config.inset_shader",
                LumenizedForgeConfig.getInsertShaderInfo()::get,
                LumenizedForgeConfig.getInsertShaderInfo()::set,
                Services.PLATFORM::isDevelopmentEnvironment));

        return new OptionPage(
                OptionIdentifier.create(new ResourceLocation("lumenized", "settings")),
                Component.translatable("lumenized.screen.config.title"),
                com.google.common.collect.ImmutableList.of(group.build()));
    }

    private static Option<Boolean> booleanOption(String id, String name, java.util.function.BooleanSupplier getter,
                                                  java.util.function.Consumer<Boolean> setter) {
        return booleanOption(id, name, getter, setter, () -> true);
    }

    private static Option<Boolean> booleanOption(String id, String name, java.util.function.BooleanSupplier getter,
                                                  java.util.function.Consumer<Boolean> setter,
                                                  java.util.function.BooleanSupplier enabled) {
        return OptionImpl.createBuilder(Boolean.class, STORAGE)
                .setId(new ResourceLocation("lumenized", id))
                .setName(Component.translatable(name))
                .setTooltip(Component.translatable(name + ".tooltip"))
                .setBinding((ignored, value) -> setter.accept(value), ignored -> getter.getAsBoolean())
                .setControl(TickBoxControl::new)
                .setEnabledPredicate(enabled)
                .setImpact(OptionImpact.VARIES)
                .build();
    }

    private static Option<Integer> integerOption(String id, String name, java.util.function.IntSupplier getter,
                                                  java.util.function.IntConsumer setter, int min, int max) {
        return OptionImpl.createBuilder(Integer.class, STORAGE)
                .setId(new ResourceLocation("lumenized", id))
                .setName(Component.translatable(name))
                .setTooltip(Component.translatable(name + ".tooltip"))
                .setBinding((ignored, value) -> setter.accept(value), ignored -> getter.getAsInt())
                .setControl(option -> new SliderControl(option, min, max, 1, ControlValueFormatter.number()))
                .setImpact(OptionImpact.VARIES)
                .build();
    }
}
