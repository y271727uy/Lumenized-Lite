package com.y271727uy.lumenized.comp.iris;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ShaderpackInjections {
    private static boolean initialized;

    public static synchronized void injectShaders() {
        if (initialized) return;
        injectTerrain();
        initialized = true;
    }

    private static void injectTerrain() {
        // BSL
        ShaderpackInjection.TERRAIN.registerTerrainVshInjection(contains("BSL Shaders"), s -> supportsLegacyLightmap(s) ? injectBefore(s, "if (mc_Entity.x >= 10200 && mc_Entity.x < 10300)", bloomUV("mat = 3.0;")) : s);
        // AstraLex
        ShaderpackInjection.TERRAIN.registerTerrainVshInjection(contains("_   ___ _____ ___    _   _    _____  __"), s -> supportsLegacyLightmap(s) ? injectBefore(s, "if (mc_Entity.x == 10269 || mc_Entity.x == 10273 || mc_Entity.x == 10274)", bloomUV("mat = 4.0;")) : s);
        // Complementary
        ShaderpackInjection.TERRAIN.registerTerrainVshInjection(contains("Complementary"), s -> supportsLegacyLightmap(s) ? injectAfter(s, "mat = int(mc_Entity.x + 0.5);", bloomUV("mat = 11000;")) : s);
        ShaderpackInjection.TERRAIN.registerTerrainFshInjection(contains("Complementary"), s -> hasVersion(s) && hasAll(s, "lmCoordM", "mat", "emission") ? injectBefore(s, "vec2 lmCoordM = lmCoord;", "if (mat == 11000) emission = 4.0;") : s);
        // Super Duper Vanilla
        ShaderpackInjection.TERRAIN.registerTerrainVshInjection(contains("Super Duper Vanilla"), s -> supportsLegacyLightmap(s) ? injectAfter(s, "blockId = int(mc_Entity.x);", bloomUV("blockId = 11000;")) : s);
        ShaderpackInjection.TERRAIN.registerTerrainFshInjection(contains("Super Duper Vanilla"), s -> hasVersion(s) && hasAll(s, "blockId", "material.albedo.rgb", "material.emissive", "material.smoothness") ? injectBefore(s, "material.albedo.rgb = toLinear(material.albedo.rgb);", "if (blockId == 11000) {material.emissive = 0.5;material.smoothness = 0.9;}") : s);
    }

    private static Predicate<String> contains(String str) {
        return s -> s.contains(str);
    }

    private static String stripComments(String source) {
        return source
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)//.*$", "");
    }

    private static boolean hasVersion(String source) {
        var version = Pattern.compile("(?m)^\\s*#version\\s+\\d+\\b").matcher(stripComments(source));
        return version.find() && !version.find();
    }

    private static boolean hasIdentifier(String source, String symbol) {
        return Pattern.compile("(?s).*\\b" + Pattern.quote(symbol) + "\\b.*").matcher(source).matches();
    }

    private static boolean hasAll(String source, String... symbols) {
        String uncommented = stripComments(source);
        for (String symbol : symbols) {
            boolean identifier = symbol.matches("[A-Za-z_][A-Za-z0-9_]*");
            boolean present = identifier
                    ? hasIdentifier(uncommented, symbol)
                    : uncommented.contains(symbol);
            if (!present) return false;
        }
        return true;
    }

    private static boolean supportsLegacyLightmap(String source) {
        String uncommented = stripComments(source);
        if (!hasIdentifier(uncommented, "gl_MultiTexCoord1")) return false;
        var version = Pattern.compile("(?im)^\\s*#version\\s+(\\d+)(?:\\s+([A-Za-z]+))?\\b").matcher(uncommented);
        if (!version.find()) return false;
        if (version.find()) return false;
        int number = Integer.parseInt(version.group(1));
        String profile = version.group(2);
        if ("core".equalsIgnoreCase(profile) || "es".equalsIgnoreCase(profile)) return false;
        return number < 330 || "compatibility".equalsIgnoreCase(profile);
    }

    private static String injectBefore(String content, String before, String injection) {
        if (content.contains(injection)) return content;
        int index = content.indexOf(before);
        if (index == -1) return content;
        return content.substring(0, index) + injection + content.substring(index);
    }

    private static String injectAfter(String content, String after, String injection) {
        if (content.contains(injection)) return content;
        int index = content.indexOf(after);
        if (index == -1) return content;
        return content.substring(0, index + after.length()) + injection + content.substring(index + after.length());
    }

    private static String bloomUV(String injection) {
        return "if((int(gl_MultiTexCoord1.x) & 0x100) != 0) {%s}".formatted(injection);
    }
}
