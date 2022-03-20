@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrate.client.renderer

import dev.adirelle.adicrate.block.entity.CrateBlockEntity
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Quaternion
import net.minecraft.util.math.Vec3f

@Environment(CLIENT)
class CrateRenderer(private val ctx: Context) : BlockEntityRenderer<CrateBlockEntity> {

    override fun render(
        crate: CrateBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val frontLight = WorldRenderer.getLightmapCoordinates(crate.world, crate.pos.offset(crate.facing))

        matrices.withNested {
            matrices.translate(0.5, 0.5, 0.5)
            matrices.multiply(Vec3f.NEGATIVE_Y.getDegreesQuaternion(crate.facing.opposite.asRotation()))

            val resource = crate.storage.resource
            if (!resource.isBlank) {
                matrices.withNested {
                    renderItem(resource.toStack(), matrices, vertexConsumers, frontLight, overlay)
                }
            }

            matrices.withNested {
                renderAmount(crate.storage.amount, matrices, vertexConsumers, frontLight)
            }
        }
    }

    private fun renderItem(
        stack: ItemStack,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val itemRenderer = MinecraftClient.getInstance().itemRenderer

        val model = itemRenderer.getModel(stack, null, null, 0)
        if (model.hasDepth()) {
            matrices.translate(0.0, 0.0, -0.51)
            matrices.scale(0.37f, 0.37f, 0.01f)
            matrices.multiply(Quaternion(-30f, 45f, 0f, true))
        } else {
            matrices.translate(0.0, 0.0, -0.5)
            matrices.scale(0.5f, 0.5f, 0.5f)
        }

        itemRenderer.renderItem(
            stack,
            ModelTransformation.Mode.NONE,
            light,
            overlay,
            matrices,
            vertexConsumers,
            0
        )
    }

    private fun renderAmount(
        amount: Long,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {
        matrices.translate(0.0, -0.45, -0.505)
        matrices.scale(-0.02f, -0.02f, -0.02f)

        val text = String.format("% 4d", amount)
        val xPosition = -ctx.textRenderer.getWidth(text).toFloat() / 2
        ctx.textRenderer.draw(
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
    }

    private inline fun MatrixStack.withNested(block: () -> Unit) {
        push()
        try {
            block()
        } finally {
            pop()
        }
    }
}
