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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import grondag.canvas.buffer.input.CanvasImmediate;
import grondag.canvas.mixinterface.LevelRendererExt;
import grondag.canvas.mixinterface.RenderBuffersExt;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.shader.data.ContextFlagState;

@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphics {
	@Shadow @Final private PoseStack pose;

	@Unique private MultiBufferSource.BufferSource vanillaBufferSource;
	@Unique private boolean vanillaInvoked = false;

	@Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V", at = @At("RETURN"))
	private void afterNew(Minecraft minecraft, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, CallbackInfo ci) {
		final var cwr = CanvasWorldRenderer.instance();

		if (cwr != null && bufferSource instanceof CanvasImmediate) {
			vanillaBufferSource = ((RenderBuffersExt) ((LevelRendererExt) cwr).canvas_bufferBuilders()).canvas_getBufferSource();
		} else {
			vanillaBufferSource = bufferSource;
		}
	}

	@ModifyArg(
			method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I"),
			index = 6)
	private MultiBufferSource onDrawStringA(MultiBufferSource multiBufferSource) {
		vanillaInvoked = true;
		return vanillaBufferSource;
	}

	@ModifyArg(
			method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I"),
			index = 6)
	private MultiBufferSource onDrawStringB(MultiBufferSource multiBufferSource) {
		vanillaInvoked = true;
		return vanillaBufferSource;
	}

	@ModifyArg(
			method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;renderText(Lnet/minecraft/client/gui/Font;IILorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"),
			index = 4)
	private MultiBufferSource.BufferSource onRenderTooltipText(MultiBufferSource.BufferSource bufferSource) {
		vanillaInvoked = true;
		return vanillaBufferSource;
	}

	@Inject(
			method = "flush",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V",
					shift = At.Shift.AFTER))
	private void afterEndBatch(CallbackInfo ci) {
		if (vanillaInvoked) {
			vanillaInvoked = false;
			vanillaBufferSource.endBatch();
		}

		ContextFlagState.setRenderingEntityInGui(false);
	}

	@Inject(
			method = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", shift = At.Shift.AFTER))
	private void afterPoseScale(LivingEntity livingEntity, Level level, ItemStack itemStack, int i, int j, int k, int l, CallbackInfo ci) {
		// 1.20.5: GUI Item shading hack
		pose.last().normal().scale(1.0f, -1.0f, 1.0f);
	}
}
