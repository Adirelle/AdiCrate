@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.client.renderer

import dev.adirelle.adicrate.AdiCrate
import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3f

@Environment(CLIENT)
class CrateRenderer(private val ctx: Context) : BlockEntityRenderer<CrateBlockEntity> {

    private val LOGGER = AdiCrate.LOGGER

    override fun render(
        crate: CrateBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val world = crate.world?.takeIf { it.isClient } ?: return
        val direction = crate.facing

        matrices.push()

        matrices.translate(0.5, 0.5, 0.5)
        matrices.multiply(Vec3f.NEGATIVE_Y.getDegreesQuaternion(direction.opposite.asRotation()))

        crate.storage.resource
            .takeUnless { it.isBlank }
            ?.let { resource ->
                // Render Item
                matrices.push()
                matrices.translate(0.0, 0.0, -0.5)
                matrices.scale(0.7f, 0.7f, 0.001f)

                val lightAbove = WorldRenderer.getLightmapCoordinates(world, crate.pos.offset(direction))
                MinecraftClient.getInstance().itemRenderer
                    .renderItem(
                        resource.toStack(),
                        ModelTransformation.Mode.FIXED,
                        lightAbove,
                        OverlayTexture.DEFAULT_UV,
                        matrices,
                        vertexConsumers,
                        0
                    )
                matrices.pop()
            }

        // Render amount
        matrices.push()
        matrices.translate(0.0, -0.5, -0.505)
        matrices.scale(-0.02f, -0.02f, -0.02f)
        val text = java.lang.String.valueOf(crate.storage.amount)
        val xPosition = (-ctx.textRenderer.getWidth(text).toFloat() / 2)
        MinecraftClient.getInstance().textRenderer.draw(
            text,
            xPosition,
            -9f,
            0,
            false,
            matrices.peek().positionMatrix,
            vertexConsumers,
            false,
            0,
            light
        )
        matrices.pop()

        // Done
        matrices.pop()
    }
}
