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

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import grondag.canvas.varia.CanvasButtonWidget;

@Mixin(OptionInstance.class)
public abstract class MixinCyclingOption {
	@Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/OptionInstance;createButton(Lnet/minecraft/client/Options;IIILjava/util/function/Consumer;)Lnet/minecraft/client/gui/components/AbstractWidget;", cancellable = true)
	private void onCreateButton(Options options, int x, int y, int width, Consumer consumer, CallbackInfoReturnable<AbstractWidget> info) {
		final OptionInstance<?> self = (OptionInstance<?>) (Object) this;

		if (self == options.graphicsMode()) {
			info.setReturnValue(new CanvasButtonWidget(x, y, width, 20, Component.translatable("config.canvas.button")));
		}
	}
}
