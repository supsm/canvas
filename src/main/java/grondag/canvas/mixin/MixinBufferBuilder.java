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

import java.nio.ByteBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

import grondag.canvas.mixinterface.BufferBuilderExt;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder implements BufferBuilderExt {
	@Shadow
	private VertexFormat format;
	@Shadow private ByteBuffer buffer;
	@Shadow private int vertices;
	@Shadow private int elementIndex;
	@Shadow private boolean building;
	@Shadow private int nextElementByte;

	@Shadow private void ensureCapacity(int i) { }

	@Override
	public boolean canvas_canSupportDirect(VertexFormat expectedFormat) {
		return building && format == expectedFormat && elementIndex == 0;
	}

	@Override
	public void canvas_putQuadDirect(int[] data) {
		assert elementIndex == 0;
		ensureCapacity(format.getVertexSize() * 4);
		vertices += 4;
		buffer.asIntBuffer().put(nextElementByte / 4, data);
		nextElementByte += data.length * 4;
	}
}
