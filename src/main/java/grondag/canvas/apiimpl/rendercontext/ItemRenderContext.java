/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.apiimpl.rendercontext;

import static grondag.canvas.buffer.format.EncoderUtils.applyItemLighting;
import static grondag.canvas.buffer.format.EncoderUtils.bufferQuad;
import static grondag.canvas.buffer.format.EncoderUtils.colorizeQuad;

import java.util.Random;
import java.util.function.Supplier;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.MaterialMap;
import io.vram.frex.api.mesh.FrexVertexConsumerProvider;
import io.vram.frex.api.model.ItemModel;

import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.ModelTransformation.Mode;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import grondag.canvas.apiimpl.mesh.QuadEditorImpl;
import grondag.canvas.buffer.format.QuadEncoders;
import grondag.canvas.buffer.input.CanvasImmediate;
import grondag.canvas.material.state.MaterialFinderImpl;
import grondag.canvas.material.state.RenderContextState;
import grondag.canvas.material.state.RenderContextState.GuiMode;
import grondag.canvas.mixin.AccessMultiPhaseParameters;
import grondag.canvas.mixin.AccessTexture;
import grondag.canvas.mixinterface.ItemRendererExt;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.MinecraftClientExt;
import grondag.canvas.mixinterface.MultiPhaseExt;
import grondag.canvas.mixinterface.ShaderExt;
import grondag.fermion.sc.concurrency.SimpleConcurrentList;

public class ItemRenderContext extends AbstractRenderContext {
	/**
	 * Value vanilla uses for item rendering.  The only sensible choice, of course.
	 */
	private static final long ITEM_RANDOM_SEED = 42L;

	private static final SimpleConcurrentList<AbstractRenderContext> LOADED = new SimpleConcurrentList<>(AbstractRenderContext.class);

	private static final Supplier<ThreadLocal<ItemRenderContext>> POOL_FACTORY = () -> ThreadLocal.withInitial(() -> {
		final ItemRenderContext result = new ItemRenderContext(((MinecraftClientExt) MinecraftClient.getInstance()).canvas_itemColors());
		LOADED.add(result);
		return result;
	});

	private static ThreadLocal<ItemRenderContext> POOL = POOL_FACTORY.get();
	private final ItemColors colorMap;
	private final Random random = new Random();
	private final Supplier<Random> randomSupplier = () -> {
		final Random result = random;
		result.setSeed(ITEM_RANDOM_SEED);
		return random;
	};
	private RenderLayer defaultRenderLayer;
	private VertexConsumer defaultConsumer;

	private int lightmap;
	private ItemStack itemStack;

	public ItemRenderContext(ItemColors colorMap) {
		super("ItemRenderContext");
		this.colorMap = colorMap;
	}

	public static void reload() {
		LOADED.forEach(c -> c.close());
		LOADED.clear();
		POOL = POOL_FACTORY.get();
	}

	public static ItemRenderContext get() {
		return POOL.get();
	}

	@Override
	public Random random() {
		return randomSupplier.get();
	}

	@Override
	public boolean defaultAo() {
		return false;
	}

	@Override
	public BlockState blockState() {
		return null;
	}

	@Override
	public int indexedColor(int colorIndex) {
		return colorIndex == -1 ? -1 : (colorMap.getColor(itemStack, colorIndex) | 0xFF000000);
	}

	@Override
	public int brightness() {
		return lightmap;
	}

	@Override
	public int flatBrightness(QuadEditorImpl quad) {
		return 0;
	}

	/**
	 * True when drawing to GUI or first person perspective.
	 */
	private boolean drawTranslucencyDirectToMainTarget;

	/**
	 * When false, assume item models are generated and should be rendered with cutout enabled if blend mode is translucent.
	 * This prevents
	 */
	private boolean isBlockItem;

	private boolean isGui;

	/**
	 * True for generated models when in GUI and diffuse shading shouldn't be used.
	 * True only when isGui is true;
	 */
	private boolean isFrontLit;

	private boolean hasGlint;

	public void renderItem(ItemModels models, ItemStack stack, Mode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model) {
		if (stack.isEmpty()) return;

		lightmap = light;
		this.overlay = overlay;
		itemStack = stack;
		isBlockItem = stack.getItem() instanceof BlockItem;
		materialMap = MaterialMap.get(itemStack);
		isGui = renderMode == ModelTransformation.Mode.GUI;
		isFrontLit = isGui && !model.isSideLit();
		hasGlint = stack.hasGlint();
		matrices.push();
		final boolean detachedPerspective = renderMode == ModelTransformation.Mode.GUI || renderMode == ModelTransformation.Mode.GROUND || renderMode == ModelTransformation.Mode.FIXED;

		if (detachedPerspective) {
			if (stack.isOf(Items.TRIDENT)) {
				model = models.getModelManager().getModel(new ModelIdentifier("minecraft:trident#inventory"));
			} else if (stack.isOf(Items.SPYGLASS)) {
				model = models.getModelManager().getModel(new ModelIdentifier("minecraft:spyglass#inventory"));
			}
		}

		// PERF: optimize matrix stack operations
		model.getTransformation().getTransformation(renderMode).apply(leftHanded, matrices);
		matrices.translate(-0.5D, -0.5D, -0.5D);

		matrix = matrices.peek().getModel();
		normalMatrix = (Matrix3fExt) (Object) matrices.peek().getNormal();

		if (model.isBuiltin() || stack.getItem() == Items.TRIDENT && !detachedPerspective) {
			final BuiltinModelItemRenderer builtInRenderer = ((ItemRendererExt) MinecraftClient.getInstance().getItemRenderer()).canvas_builtinModelItemRenderer();

			if (isGui && vertexConsumers instanceof CanvasImmediate) {
				final RenderContextState context = ((CanvasImmediate) vertexConsumers).contextState;
				context.guiMode(isBlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof AbstractBannerBlock ? GuiMode.GUI_FRONT_LIT : GuiMode.NORMAL);
				builtInRenderer.render(stack, renderMode, matrices, vertexConsumers, light, overlay);
				context.guiMode(GuiMode.NORMAL);
			} else {
				builtInRenderer.render(stack, renderMode, matrices, vertexConsumers, light, overlay);
			}
		} else {
			drawTranslucencyDirectToMainTarget = isGui || renderMode.isFirstPerson() || !isBlockItem;
			defaultRenderLayer = RenderLayers.getItemLayer(stack, drawTranslucencyDirectToMainTarget);
			defaultBlendMode = inferDefaultItemPreset(defaultRenderLayer);

			if (((vertexConsumers instanceof CanvasImmediate))) {
				collectors = ((CanvasImmediate) vertexConsumers).collectors;
			} else {
				collectors = null;
				defaultConsumer = vertexConsumers.getBuffer(defaultRenderLayer);
			}

			((ItemModel) model).renderAsItem(itemStack, renderMode, this);
		}

		matrices.pop();
	}

	@Override
	protected void adjustMaterial() {
		final MaterialFinderImpl finder = this.finder;

		finder.enableGlint(hasGlint);

		int preset = finder.preset();

		// fully specific renderable material
		if (preset == MaterialConstants.PRESET_NONE) return;

		if (preset == MaterialConstants.PRESET_DEFAULT) {
			preset = defaultBlendMode;
			finder.preset(MaterialConstants.PRESET_NONE);
		}

		switch (preset) {
			case MaterialConstants.PRESET_CUTOUT:
				finder.transparency(MaterialConstants.TRANSPARENCY_NONE)
					.cutout(MaterialConstants.CUTOUT_HALF)
					.unmipped(true)
					.target(MaterialConstants.TARGET_MAIN)
					.sorted(false);
				break;
			case MaterialConstants.PRESET_CUTOUT_MIPPED:
				finder.transparency(MaterialConstants.TRANSPARENCY_NONE)
					.cutout(MaterialConstants.CUTOUT_HALF)
					.unmipped(false)
					.target(MaterialConstants.TARGET_MAIN)
					.sorted(false);
				break;
			case MaterialConstants.PRESET_TRANSLUCENT:
				// Note on glint rendering
				// Glint renders use EQUALS depth test.
				// This makes it important that
				//   1) geometry is the same
				//   2) cutout is enabled for generated models so depth buffer isn't updated
				// 1 is easily solved by rendering twice with same vertex data
				// 2 has to be finessed because blend mode = TRANSLUCENT doesn't make it clear cutout is needed.
				// The code below is an ugly hack - need a better way

				finder.transparency(MaterialConstants.TRANSPARENCY_TRANSLUCENT)
					.cutout(isBlockItem ? MaterialConstants.CUTOUT_NONE : MaterialConstants.CUTOUT_TENTH)
					.unmipped(false)
					.target(drawTranslucencyDirectToMainTarget ? MaterialConstants.TARGET_MAIN : MaterialConstants.TARGET_ENTITIES)
					.sorted(!drawTranslucencyDirectToMainTarget);
				break;
			case MaterialConstants.PRESET_SOLID:
				finder.transparency(MaterialConstants.TRANSPARENCY_NONE)
					.cutout(MaterialConstants.CUTOUT_NONE)
					.unmipped(false)
					.target(MaterialConstants.TARGET_MAIN)
					.sorted(false);
				break;
			default:
				assert false : "Unhandled blend mode";
		}

		if (isGui && isFrontLit) {
			finder.disableDiffuse(true);
		}

		finder.disableAo(true);
	}

	@Override
	public void computeAo(QuadEditorImpl quad) {
		// NOOP
	}

	@Override
	public void computeFlat(QuadEditorImpl quad) {
		computeFlatSimple(quad);
	}

	@Override
	protected void encodeQuad(QuadEditorImpl quad) {
		colorizeQuad(quad, this);
		applyItemLighting(quad, this);

		if (collectors == null) {
			bufferQuad(quad, this, defaultConsumer);
		} else {
			QuadEncoders.STANDARD_ENCODER.encode(quad, this, collectors.get(quad.material()));
		}
	}

	static int inferDefaultItemPreset(RenderLayer layer) {
		final AccessMultiPhaseParameters params = ((MultiPhaseExt) layer).canvas_phases();

		if (params.getTransparency() == RenderPhase.TRANSLUCENT_TRANSPARENCY) {
			return MaterialConstants.PRESET_TRANSLUCENT;
		} else if (((ShaderExt) params.getShader()).canvas_shaderData().cutout != MaterialConstants.CUTOUT_NONE) {
			final AccessTexture tex = (AccessTexture) params.getTexture();
			return tex.getMipmap() ? MaterialConstants.PRESET_CUTOUT_MIPPED : MaterialConstants.PRESET_CUTOUT;
		} else {
			return MaterialConstants.PRESET_SOLID;
		}
	}

	@Override
	public FrexVertexConsumerProvider vertexConsumers() {
		// WIP implement
		return null;
	}

	@Override
	public MatrixStack matrixStack() {
		// WIP implement
		return null;
	}
}
