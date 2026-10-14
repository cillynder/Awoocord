package com.aliucord.coreplugins.componentsv2.views

import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.aliucord.Logger
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.asAttachment
import com.aliucord.coreplugins.componentsv2.models.ThumbnailMessageComponent
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.ViewUtils.addTo
import com.discord.utilities.color.ColorCompat
import com.discord.utilities.embed.EmbedResourceUtils
import com.discord.utilities.images.MGImages
import com.discord.widgets.botuikit.ComponentProvider
import com.discord.widgets.botuikit.views.ComponentActionListener
import com.discord.widgets.botuikit.views.ComponentView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRow
import com.discord.widgets.media.WidgetMedia
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R

class ThumbnailComponentView(ctx: Context) : ConstraintLayout(ctx), ComponentView<ThumbnailMessageComponent> {
    override fun type() = ComponentV2Type.THUMBNAIL

    private val embedThumbnailMaxSize = (ctx.resources.getDimension(R.d.embed_thumbnail_max_size) * 1.5).toInt()

    companion object {
        private val imageViewId = View.generateViewId()
    }
    private lateinit var imageView: SimpleDraweeView
    private lateinit var spoilerView: SpoilerView

    init {
        MaterialCardView(ctx).addTo(this) {
            radius = 8.dp.toFloat()
            elevation = 0f
            setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundPrimary))
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            ConstraintLayout(ctx).addTo(this) {
                layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                imageView = SimpleDraweeView(ctx, null, 0, R.i.UiKit_ImageView).addTo(this) {
                    id = imageViewId
                }
                spoilerView = SpoilerView(ctx, 2).addTo(this) {
                    layoutParams = SpoilerView.constraintLayoutParamsAround(imageViewId)
                }
            }
        }
    }

    // Reference: WidgetChatListAdapterItemEmbed.configureEmbedThumbnail
    override fun configure(component: ThumbnailMessageComponent, provider: ComponentProvider, listener: ComponentActionListener) {
        val item = listener as WidgetChatListAdapterItemBotComponentRow
        val entry = item.entry
        if (entry !is BotUiComponentV2Entry) {
            Logger("ComponentsV2").warn("configured thumbnail with non-v2 entry")
            return
        }

        val attachment = component.media.asAttachment()
        val (width, height) = EmbedResourceUtils.INSTANCE.calculateScaledSize(
            component.media.width,
            component.media.height,
            embedThumbnailMaxSize,
            embedThumbnailMaxSize,
            resources,
            0
        )
        imageView.apply {
            if (layoutParams.width != width || layoutParams.height != height)
                layoutParams = layoutParams.apply {
                    this.width = width
                    this.height = height
                }
            MGImages.`setImage$default`(
                this,
                EmbedResourceUtils.INSTANCE.getPreviewUrls(component.media.proxyUrl, width, height, true), // z2: shouldAnimate
                0,
                0,
                false,
                null,
                null,
                null,
                252,
                null
            )
            setOnClickListener {
                WidgetMedia.Companion!!.launch(context, attachment);
            }
        }

        spoilerView.configure(entry, component)
    }
}
