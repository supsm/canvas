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

package io.vram.canvas.mixin.fabric;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;

import grondag.canvas.terrain.region.input.InputRegion;

/** Attached Fabric API interface to our render regions when needed for compat. */
@Mixin(InputRegion.class)
public class MixinInputRegion implements FabricBlockView {
	@Shadow
	protected Level world;

	@Override
	public boolean hasBiomes() {
		return true;
	}

	@Override
	public Holder<Biome> getBiomeFabric(BlockPos pos) {
		return world.getBiome(pos);
	}
}
