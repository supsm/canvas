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

package grondag.canvas.shader.data;

import grondag.canvas.material.property.TargetRenderState;

/**
 * Data for context flag uniform.
 */
public class ContextFlagState {
	private static boolean renderingHand = false;
	private static boolean needUploadGuiMatrix = true;
	private static boolean renderingEntity = false;
	private static boolean renderingEntityInGui = false;

	public static void setRenderingHand(boolean value) {
		renderingHand = value;
		needUploadGuiMatrix = true;
	}

	public static boolean renderingHand() {
		return renderingHand;
	}

	public static boolean needUploadGuiMatrix() {
		return needUploadGuiMatrix;
	}

	public static void markGuiMatrixUploaded() {
		needUploadGuiMatrix = false;
	}

	public static void setRenderingEntity(boolean value) {
		renderingEntity = value;
	}

	public static void setRenderingEntityInGui(boolean value) {
		renderingEntityInGui = value;
	}

	public static boolean renderingEntityAny(int target) {
		return renderingEntity || renderingEntityInGui || TargetRenderState.fromIndex(target) == TargetRenderState.ENTITIES;
	}
}
