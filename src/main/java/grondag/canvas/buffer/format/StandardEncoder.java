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

package grondag.canvas.buffer.format;

import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_FIRST_VERTEX_TANGENT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.MESH_VERTEX_STRIDE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.UV_EXTRA_PRECISION;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.UV_ROUNDING_BIT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_COLOR;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_NORMAL;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_U;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_V;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_X;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_Y;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_Z;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import io.vram.frex.api.math.FrexMathUtil;
import io.vram.frex.api.model.InputContext;
import io.vram.frex.base.renderer.context.input.AbsentInputContext;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;
import io.vram.frex.base.renderer.mesh.MeshEncodingHelper;

import grondag.canvas.buffer.input.VertexCollector;
import grondag.canvas.material.state.CanvasRenderMaterial;

public class StandardEncoder {
	private StandardEncoder() { }

	public static void encodeQuad(BaseQuadEmitter quad, InputContext inputContext, VertexCollector buff) {
		final var matrixStack = inputContext.matrixStack();
		final Matrix4f matrix = matrixStack.modelMatrix();
		final Matrix3f normalMatrix = matrixStack.normalMatrix();
		final boolean isContextPresent = inputContext != AbsentInputContext.INSTANCE;

		final CanvasRenderMaterial mat = (CanvasRenderMaterial) quad.material();
		final boolean unlit = mat.unlit();

		final int quadNormalFlags = quad.normalFlags();
		// don't retrieve if won't be used
		final int faceNormal = quadNormalFlags == 0b1111 ? 0 : quad.packedFaceNormal();
		// bit 1 is set if normal Z component is negative
		int normalSignBit = 0;
		int packedNormal = 0;
		int transformedNormal = 0;

		final int quadTangetFlags = quad.tangentFlags();
		final int faceTangent = quadTangetFlags == 0b1111 ? 0 : quad.packedFaceTanget();
		// bit 1 is set if tangent Z component is negative
		int tangentSignBit = 0;
		int tangentInverseBit = 0;
		int packedTangent = 0;
		int transformedTangent = 0;

		final int material = mat.materialIndexer().index(quad.spriteId()) << 16;

		final int[] target = buff.target();
		final int baseSourceIndex = quad.vertexStart();
		final int[] source = quad.data();

		for (int i = 0; i < 4; i++) {
			final int vertexMask = 1 << i;
			final int fromIndex = baseSourceIndex + i * MESH_VERTEX_STRIDE;
			final int toIndex = i * CanvasVertexFormats.STANDARD_VERTEX_STRIDE;

			final int p = ((quadNormalFlags & vertexMask) == 0) ? faceNormal : source[fromIndex + VERTEX_NORMAL];

			if (p != packedNormal) {
				packedNormal = p;
				transformedNormal = isContextPresent ? FrexMathUtil.transformPacked3f(normalMatrix, packedNormal) : packedNormal;
				normalSignBit = (transformedNormal >>> 23) & 1;
				transformedNormal &= 0xFFFF;
			}

			final int t = ((quadTangetFlags & vertexMask) == 0) ? faceTangent : source[baseSourceIndex + i + HEADER_FIRST_VERTEX_TANGENT];

			if (t != packedTangent) {
				packedTangent = t;
				transformedTangent = isContextPresent ? FrexMathUtil.transformPacked3f(normalMatrix, packedTangent) : packedTangent;
				tangentSignBit = (transformedTangent >>> 23) & 1;
				tangentInverseBit = (transformedTangent << 7) & 0x80000000;
				transformedTangent = (transformedTangent & 0xFFFF) << 16;
			}

			final float x = Float.intBitsToFloat(source[fromIndex + VERTEX_X]);
			final float y = Float.intBitsToFloat(source[fromIndex + VERTEX_Y]);
			final float z = Float.intBitsToFloat(source[fromIndex + VERTEX_Z]);

			final float xOut = matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30();
			final float yOut = matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31();
			final float zOut = matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32();

			target[toIndex] = Float.floatToRawIntBits(xOut);
			target[toIndex + 1] = Float.floatToRawIntBits(yOut);
			target[toIndex + 2] = Float.floatToRawIntBits(zOut);

			target[toIndex + 3] = source[fromIndex + VERTEX_COLOR];

			target[toIndex + 4] = (source[fromIndex + VERTEX_U] + UV_ROUNDING_BIT) >> UV_EXTRA_PRECISION
				| ((source[fromIndex + VERTEX_V] + UV_ROUNDING_BIT) >> UV_EXTRA_PRECISION << 16);

			// TODO: should probably pass unlit as a flag vs forcing lightmap
			final int packedLight = unlit ? MeshEncodingHelper.FULL_BRIGHTNESS : source[fromIndex + VERTEX_LIGHTMAP];
			final int blockLight = (packedLight & 0xFE) | normalSignBit;
			final int skyLight = ((packedLight >> 16) & 0xFE) | tangentSignBit;
			target[toIndex + 5] = blockLight | (skyLight << 8) | material | tangentInverseBit;

			target[toIndex + 6] = transformedNormal | transformedTangent;
		}

		buff.commit(quad.effectiveCullFaceId(), mat.castShadows());
	}
}
