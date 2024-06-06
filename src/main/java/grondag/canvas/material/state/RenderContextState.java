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

package grondag.canvas.material.state;

import java.util.Stack;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.MaterialMap;

public class RenderContextState {
	private final MaterialFinder finder = MaterialFinder.newInstance();

	private final Stack<State> states = new Stack<>();

	public RenderContextState() {
	}

	public void push(@Nonnull Entity entity) {
		states.push(new State(MaterialMap.get(entity.getType()), entity, true, GuiMode.NONE));
	}

	public void push(@Nonnull BlockEntity blockEntity) {
		states.push(new State(MaterialMap.get(blockEntity.getType()), blockEntity.getBlockState(), true, GuiMode.NONE));
	}

	/**
	 * For items with custom renderer.
	 *
	 * @param itemStack the item stack
	 */
	public void push(@Nonnull ItemStack itemStack, boolean isGui, boolean isFrontLit) {
		final GuiMode mode = isGui ? (isFrontLit ? GuiMode.FRONT_LIT : GuiMode.NORMAL) : GuiMode.NONE;
		states.push(new State(MaterialMap.get(itemStack), null, itemStack.is(Items.TRIDENT), mode));
	}

	public void pop() {
		states.pop();
	}

	/**
	 * Prevent unwanted state/overflow due to conflicting hooks, etc.
	 */
	public void clear() {
		states.clear();
	}

	@SuppressWarnings("unchecked")
	public CanvasRenderMaterial mapMaterial(CanvasRenderMaterial mat) {
		if (states.empty()) {
			return mat;
		} else {
			final State state = states.peek();

			finder.copyFrom(mat);

			state.map.map(finder, state.testObj);

			finder.glintEntity(state.glintEntity);

			if (state.testObj == null) {
				finder.textureIndex(mat.textureIndex());
			}

			state.guiMode.apply(finder);

			return (CanvasRenderMaterial) finder.find();
		}
	}

	@SuppressWarnings("rawtypes")
	private record State (MaterialMap map, Object testObj, boolean glintEntity, GuiMode guiMode) { }

	private enum GuiMode {
		NONE() {
			@Override void apply(MaterialFinder finder) { }
		},
		NORMAL() {
			@Override void apply(MaterialFinder finder) {
				finder.fog(false);
			}
		},
		FRONT_LIT() {
			@Override void apply(MaterialFinder finder) {
				finder.fog(false).disableDiffuse(true);
			}
		};

		abstract void apply(MaterialFinder finder);
	}
}
