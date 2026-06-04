package com.y271727uy.lumenized;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * put fields that related to mc stuffs to avoid class loading to early
 */
public class LumenizedFields {
    public static final KeyMapping recordScreenColor = new KeyMapping("lumenized.key.pickColor", InputConstants.KEY_V, "key.categories.misc");
}
