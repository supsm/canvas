/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.mixin;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;

import grondag.canvas.mixinterface.BufferBuilderExt;

@Mixin(BakedGlyph.class)
public abstract class MixinBakedGlyph {
	@Shadow private float u0;
	@Shadow private float u1;
	@Shadow private float v0;
	@Shadow private float v1;
	@Shadow private float left;
	@Shadow private float right;
	@Shadow private float up;
	@Shadow private float down;

	private static final Vector3f pos = new Vector3f();
	// NB: size in bytes is size of integer array for whole quad
	private static final int[] quadData = new int[DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP.getVertexSize()];

	// PERF: consider handling drawRectangle also

	/**
	 * @author grondag
	 * @reason performance; calls are too frequent and parameter stack too large for inject to perform well
	 */
	@Overwrite
	public void render(boolean oblique, float x, float y, Matrix4f matrix4f, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int lightmap) {
		final float x0 = x + left;
		final float x1 = x + right;
		final float y0 = y + up;
		final float y1 = y + down;
		final float xOffset0 = oblique ? 1.0F - 0.25F * up : 0.0F;
		final float xOffset1 = oblique ? 1.0F - 0.25F * down : 0.0F;

		if (vertexConsumer instanceof final BufferBuilderExt extBuilder
				&& extBuilder.canvas_canSupportDirect(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
				&& RenderSystem.isOnRenderThread() // This last is because we are using static vars
		) {
			final int color = (int) (red * 255.0F) | ((int) (green * 255.0F) << 8) | ((int) (blue * 255.0F) << 16) | ((int) (alpha * 255.0F) << 24);
			int i = 0;

			pos.set(x0 + xOffset0, y0, 0.0F).mulPosition(matrix4f);
			quadData[i++] = Float.floatToRawIntBits(pos.x());
			quadData[i++] = Float.floatToRawIntBits(pos.y());
			quadData[i++] = Float.floatToRawIntBits(pos.z());
			quadData[i++] = color;
			quadData[i++] = Float.floatToRawIntBits(u0);
			quadData[i++] = Float.floatToRawIntBits(v0);
			quadData[i++] = lightmap;

			pos.set(x0 + xOffset1, y1, 0.0F).mulPosition(matrix4f);
			quadData[i++] = Float.floatToRawIntBits(pos.x());
			quadData[i++] = Float.floatToRawIntBits(pos.y());
			quadData[i++] = Float.floatToRawIntBits(pos.z());
			quadData[i++] = color;
			quadData[i++] = Float.floatToRawIntBits(u0);
			quadData[i++] = Float.floatToRawIntBits(v1);
			quadData[i++] = lightmap;

			pos.set(x1 + xOffset1, y1, 0.0F).mulPosition(matrix4f);
			quadData[i++] = Float.floatToRawIntBits(pos.x());
			quadData[i++] = Float.floatToRawIntBits(pos.y());
			quadData[i++] = Float.floatToRawIntBits(pos.z());
			quadData[i++] = color;
			quadData[i++] = Float.floatToRawIntBits(u1);
			quadData[i++] = Float.floatToRawIntBits(v1);
			quadData[i++] = lightmap;

			pos.set(x1 + xOffset0, y0, 0.0F).mulPosition(matrix4f);
			quadData[i++] = Float.floatToRawIntBits(pos.x());
			quadData[i++] = Float.floatToRawIntBits(pos.y());
			quadData[i++] = Float.floatToRawIntBits(pos.z());
			quadData[i++] = color;
			quadData[i++] = Float.floatToRawIntBits(u1);
			quadData[i++] = Float.floatToRawIntBits(v0);
			quadData[i++] = lightmap;

			assert i == quadData.length;
			extBuilder.canvas_putQuadDirect(quadData);
		} else {
			vertexConsumer.vertex(matrix4f, x0 + xOffset0, y0, 0.0F).color(red, green, blue, alpha).uv(u0, v0).uv2(lightmap).endVertex();
			vertexConsumer.vertex(matrix4f, x0 + xOffset1, y1, 0.0F).color(red, green, blue, alpha).uv(u0, v1).uv2(lightmap).endVertex();
			vertexConsumer.vertex(matrix4f, x1 + xOffset1, y1, 0.0F).color(red, green, blue, alpha).uv(u1, v1).uv2(lightmap).endVertex();
			vertexConsumer.vertex(matrix4f, x1 + xOffset0, y0, 0.0F).color(red, green, blue, alpha).uv(u1, v0).uv2(lightmap).endVertex();
		}
	}
}
