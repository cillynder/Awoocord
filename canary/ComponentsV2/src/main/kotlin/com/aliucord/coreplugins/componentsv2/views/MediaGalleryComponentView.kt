package com.aliucord.coreplugins.componentsv2.views

import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import com.aliucord.Logger
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.asAttachment
import com.aliucord.coreplugins.componentsv2.models.MediaGalleryMessageComponent
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.ViewUtils.addTo
import com.aliucord.widgets.LinearLayout
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.height
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.width
import com.discord.api.message.attachment.MessageAttachment
import com.discord.utilities.color.ColorCompat
import com.discord.utilities.display.DisplayUtils
import com.discord.utilities.embed.EmbedResourceUtils
import com.discord.widgets.botuikit.ComponentProvider
import com.discord.widgets.botuikit.views.ComponentActionListener
import com.discord.widgets.botuikit.views.ComponentView
import com.discord.widgets.chat.list.InlineMediaView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRow
import com.discord.widgets.media.WidgetMedia
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R

class MediaGalleryComponentView(ctx: Context) : ConstraintLayout(ctx), ComponentView<MediaGalleryMessageComponent> {
    override fun type() = ComponentV2Type.MEDIA_GALLERY

    companion object {
        private val mediaViewId = View.generateViewId()
        private val maxEmbedHeight = EmbedResourceUtils.INSTANCE.maX_IMAGE_VIEW_HEIGHT_PX
    }

    private val layout = LinearLayout(ctx).addTo(this) {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topToTop = PARENT_ID
            startToStart = PARENT_ID
            endToEnd = PARENT_ID
        }
    }
    private var mediaViews: List<Pair<MessageAttachment, InlineMediaView>>? = null

    // This isn't pretty, but Discord actually does this in their code (EmbedResourceUtils.computeMaximumImageWidthPx)
    private fun calculateMaxWidth(contained: Boolean): Int {
        var maxPossibleWidth = DisplayUtils.getScreenSize(context).width() -
            resources.getDimensionPixelSize(R.d.uikit_guideline_chat) -
            resources.getDimensionPixelSize(R.d.chat_cell_horizontal_spacing_total)

        if (contained)
            maxPossibleWidth -= 15.dp

        return maxPossibleWidth.coerceAtMost(1440)
    }

    // Reference: WidgetChatListAdapterItemAttachment.configureUI
    override fun configure(component: MediaGalleryMessageComponent, provider: ComponentProvider, listener: ComponentActionListener) {
        val item = listener as WidgetChatListAdapterItemBotComponentRow
        val entry = item.entry
        if (entry !is BotUiComponentV2Entry) {
            Logger("ComponentsV2").warn("configured media gallery with non-v2 entry")
            return
        }

        val maxEmbedWidth = calculateMaxWidth(component.markedContained)
        layout.removeAllViews()
        val pendingViews = mutableListOf<Pair<MessageAttachment, InlineMediaView>>()
        component.items.forEachIndexed { index, it ->
            val attachment = it.media.asAttachment()

            val (width, height) = EmbedResourceUtils.INSTANCE.calculateScaledSize(
                attachment.width!!,
                attachment.height!!,
                maxEmbedWidth,
                maxEmbedHeight,
                resources,
                0,
            )
            MaterialCardView(context).addTo(layout) {
                radius = 8.dp.toFloat()
                elevation = 0f
                setCardBackgroundColor(ColorCompat.getThemedColor(context, R.b.colorBackgroundPrimary))
                layoutParams = android.widget.LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    topMargin = 8.dp
                }
                ConstraintLayout(context).addTo(this) {
                    layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                    val mediaView = InlineMediaView(context).addTo(this) {
                        radius = 8.dp.toFloat()
                        elevation = 0f
                        setCardBackgroundColor(ColorCompat.getThemedColor(context, R.b.colorBackgroundPrimary))
                        id = mediaViewId
                        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                            topToTop = PARENT_ID
                            startToStart = PARENT_ID
                        }
                        setOnClickListener {
                            WidgetMedia.Companion!!.launch(context, attachment);
                        }
                        updateUIWithAttachment(attachment, width, height, true)
                    }
                    val spoilerView = SpoilerView(context, 1).addTo(this) {
                        translationZ = 10f
                        layoutParams = SpoilerView.constraintLayoutParamsAround(mediaViewId)
                    }
                    pendingViews.add(attachment to mediaView)
                    spoilerView.configure(it.spoiler, entry.state, entry.message.id, Pair(component.id, "media:$index"))
                }
            }
        }
        mediaViews = pendingViews.toList()
    }
}
