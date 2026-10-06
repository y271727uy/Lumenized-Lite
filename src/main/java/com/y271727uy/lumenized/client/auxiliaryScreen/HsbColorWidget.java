package com.y271727uy.lumenized.client.auxiliaryScreen;

import com.y271727uy.lumenized.Utils;
import com.y271727uy.lumenized.LumenizedConstants;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * the color picker using HSB color space<p>
 * the vertices must be feed by counterclockwise, or they will be clipped
 */
public class HsbColorWidget extends AbstractWidget {

	/**
	 * shader used by this widget, interpolated by vsh->fsh, with HSB2RGB by fsh
	 */
	public static ShaderInstance hsbShader;
	public static final VertexFormat HSB_VERTEX_FORMAT = DefaultVertexFormat.POSITION_COLOR;

	/**
	 * the vertex format for HSB color, three four of float
	 */
	/**
	 * all supported pick mode
	 */
	private enum HSB_MODE {
		H("hue"), S("saturation"), B("brightness");
		private final String name;

		HSB_MODE(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * the length between the main and slide
	 */
	private int gap;
	/**
	 * the slide width
	 */
	private int barWidth;
	/**
	 * hue component, must range from 0f to 360f
	 */
	private float h = 204;
	/**
	 * saturation component, must range from 0f to 1f
	 */
	private float s = 0.72f;
	/**
	 * the brightness component, must range from 0f to 1f
	 */
	private float b = 0.94f;
	/**
	 * thr alpha used for draw main and slide
	 */
	private int alpha = 1;
	/**
	 * the rgb transformed from hsb color space
	 * [0x00rrggbb]
	 */
	private int rgb;

	private HSB_MODE mode = HSB_MODE.H;

	private List<Runnable> listeners = new ArrayList<>();

	public HsbColorWidget(int x, int y, int width, int height, int gap, int barWidth, Component arg) {
		super(x, y, width, height, arg);
		this.gap = gap;
		this.barWidth = barWidth;

		refreshRGB(true);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

	}

	@Override
	public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
		int base = 0xff000000 | (rgb & 0xffffff);
		guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, base);
		int barX = getX() + width + gap;
		guiGraphics.fill(barX, getY(), barX + barWidth, getY() + height, 0xff808080);
		int colorX = barX + barWidth + 40;
		guiGraphics.fill(colorX, getY(), colorX + 20, getY() + 20, base);
		Font font = Minecraft.getInstance().font;
		int strX = colorX + 30;
		int strGapY = font.lineHeight + 2;
		guiGraphics.drawString(font, "h:" + (int) h + "°", strX, getY(), 0xffffffff);
		guiGraphics.drawString(font, "s:" + (int) (s * 100) + "%", strX, getY() + strGapY, 0xffffffff);
		guiGraphics.drawString(font, "b:" + (int) (b * 100) + "%", strX, getY() + strGapY * 2, 0xffffffff);
		guiGraphics.drawString(font, "r:" + ((rgb >> 16) & 0xff), strX, getY() + strGapY * 3, 0xffffffff);
		guiGraphics.drawString(font, "g:" + ((rgb >> 8) & 0xff), strX, getY() + strGapY * 4, 0xffffffff);
		guiGraphics.drawString(font, "b:" + (rgb & 0xff), strX, getY() + strGapY * 5, 0xffffffff);
		guiGraphics.drawString(font, "mode:" + mode, strX, getY() + strGapY * 6, 0xffffffff);
	}

	/**
	 * render hsb/rgb/mode info
	 */

	/**
	 * take the slide into account
	 */
	@Override
	protected boolean clicked(double mouseX, double mouseY) {
		return this.active && this.visible &&
				mouseY >= (double) this.getY() &&
				mouseY < (double) (this.getY() + this.height) && (
				(mouseX >= (double) this.getX() && mouseX < (double) (this.getX() + this.width)) ||
						((mouseX >= (double) this.getX() + this.width + this.gap) &&
								mouseX <= (double) (this.getX() + this.width + this.gap + this.barWidth))
		);
	}

	/**
	 * change mode when right click
	 */
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			mode = switch (mode) {
				case H -> HSB_MODE.S;
				case S -> HSB_MODE.B;
				case B -> HSB_MODE.H;
			};
			return true;
		} else return super.mouseClicked(mouseX, mouseY, button);
	}

	/**
	 * calculate rgb color when hsb changed
	 */
	private void refreshRGB(boolean trigListener) {
		rgb = Utils.HSBtoRGB(h / 360f, s, b);
		if (trigListener){
			listeners.forEach(Runnable::run);
		}
	}

	/**
	 * helper method for normalized calculation, a period function with cycle of 2
	 * <p>
	 * y=x when x range from 0 to 1
	 * <p>
	 * y=2-x when x range from 1 to 2
	 *
	 * @param mouse the mouseX/Y
	 * @param pos   x/y position for widget
	 * @param size  width/height position for widget
	 * @return the normalized user friend value
	 */
	private static float normalizeMouse(double mouse, int pos, int size) {
		double x = mouse - pos;
		double y = x % size / size;
		if (y < 0) {
			x = -x;
			y = -y;
		}
		x /= size;
		return (float) (x % 2 > 1 ? 1 - y : y);
	}

	/**
	 * modify when keep pressing the mouse
	 */
	@Override
	public void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
		float normalizedX = normalizeMouse(mouseX, getX(), width);
		var isBar = mouseX - getX() > width;
		float normalizedY = normalizeMouse(mouseY, getY(), height);
		switch (mode) {
			case H -> {
				if (isBar) {
					h = (1.0f - normalizedY) * 360f;
				} else {
					s = normalizedX;
					b = 1.0f - normalizedY;
				}
			}
			case S -> {
				if (isBar) {
					s = 1.0f - normalizedY;
				} else {
					h = normalizedX * 360f;
					b = 1.0f - normalizedY;
				}
			}
			case B -> {
				if (isBar) {
					b = 1.0f - normalizedY;
				} else {
					h = normalizedX * 360f;
					s = normalizedY;
				}
			}
		}
		refreshRGB(true);
	}

	public float[] getHSB() {
		return new float[]{h, s, b};
	}

	public float[] getRGB() {
		return new float[]{FastColor.ARGB32.red(rgb) / 255f, FastColor.ARGB32.green(rgb) / 255f, FastColor.ARGB32.blue(rgb) / 255f};
	}

	public void setHSB(float[] hsb) {
		if (hsb[0] != this.h || hsb[1] != this.s || hsb[2] != this.b){
			this.h = hsb[0];
			this.s = hsb[1];
			this.b = hsb[2];
			refreshRGB(true);
		}
	}

	public void setRGB(float[] rgb) {
		var hsb = new float[3];
		Utils.RGBtoHSB(rgb, hsb);
		hsb[0] *= 360f;
		setHSB(hsb);
	}

	public int rgb() {
		return rgb;
	}

	public void registerListener(Runnable listener){
		listeners.add(listener);
	}

	/**
	 * for fabric only
	 */
	public static Pair<ShaderInstance, Consumer<ShaderInstance>> registerShaders(ResourceManager resourceManager) {
		try {
			return Pair.of(new ShaderInstance(resourceManager, ResourceLocation.fromNamespaceAndPath(LumenizedConstants.MOD_ID, "hsb_block").toString(), HSB_VERTEX_FORMAT),
					shaderInstance -> hsbShader = shaderInstance);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
