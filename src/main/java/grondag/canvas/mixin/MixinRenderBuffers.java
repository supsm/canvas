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

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderBuffers;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.input.CanvasImmediate;
import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.RenderBuffersExt;

@Mixin(RenderBuffers.class)
public class MixinRenderBuffers implements RenderBuffersExt {
	@Shadow @Final private BufferSource bufferSource;

	@Unique private BufferSource canvas_activeBufferSource;

	@ModifyArg(
			method = "<init>",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SectionBufferBuilderPool;allocate(I)Lnet/minecraft/client/renderer/SectionBufferBuilderPool;"))
	private int onInitZeroListSize(int ignored) {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: sectionBufferPool init (zero list size)");
		}

		return 0;
	}

	@Inject(at = @At("RETURN"), method = "<init>*")
	private void onNew(CallbackInfo ci) {
		canvas_activeBufferSource = bufferSource;
	}

	/**
	 * @author grondag
	 * @reason simple and reliable
	 */
	@Overwrite
	public MultiBufferSource.BufferSource bufferSource() {
		return canvas_activeBufferSource;
	}

	@Override
	public void canvas_setEntityConsumers(CanvasImmediate consumers) {
		canvas_activeBufferSource = consumers == null ? bufferSource : consumers;
	}

	@Override
	public BufferSource canvas_getBufferSource() {
		return bufferSource;
	}
}
