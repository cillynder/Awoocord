package com.aliucord.coreplugins.componentsv2.views

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.applyEmbedStyle
import com.aliucord.coreplugins.componentsv2.models.FileMessageComponent
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.ViewUtils.addTo
import com.discord.utilities.color.ColorCompat
import com.discord.utilities.drawable.DrawableCompat
import com.discord.utilities.embed.EmbedResourceUtils
import com.discord.widgets.botuikit.ComponentProvider
import com.discord.widgets.botuikit.views.ComponentActionListener
import com.discord.widgets.botuikit.views.ComponentView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRow
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R

class FileComponentView(ctx: Context) : ConstraintLayout(ctx), ComponentView<FileMessageComponent> {
    override fun type() = ComponentV2Type.FILE

    private lateinit var iconView: ImageView
    private lateinit var nameView: TextView
    private lateinit var descriptionView: TextView
    private lateinit var downloadView: ImageView

    companion object {
        private val iconId = generateViewId()
        private val nameId = generateViewId()
        private val descriptionId = generateViewId()
        private val downloadId = generateViewId()
    }

    /* Reference: widget_chat_list_adapter_item_attachment.xml:chat_list_item_attachment_card */
    init {
        MaterialCardView(ctx).addTo(this) {
            applyEmbedStyle()
            setCardBackgroundColor(ColorCompat.getThemedColor(context, R.b.colorBackgroundTertiary))
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topToTop = PARENT_ID
                bottomToBottom = PARENT_ID
                startToStart = PARENT_ID
                bottomMargin = ctx.resources.getDimensionPixelSize(R.d.chat_cell_vertical_spacing_padding)
            }
            ConstraintLayout(ctx).addTo(this) {
                setPadding(8.dp, 8.dp, 8.dp, 8.dp)
                iconView = ImageView(ctx).addTo(this) {
                    id = iconId
                    setImageResource(R.e.ic_file_unknown)
                    contentDescription = resources.getString(R.h.attachment_filename_unknown)
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        bottomToBottom = PARENT_ID
                        startToStart = PARENT_ID
                        topToTop = PARENT_ID
                    }
                }
                nameView = TextView(ctx, null, 0, R.i.UiKit_TextView_Link).addTo(this) {
                    id = nameId
                    setLineSpacing(lineSpacingExtra, 1f)
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        marginStart = 8.dp
                        horizontalBias = 0f
                        verticalChainStyle = LayoutParams.CHAIN_PACKED
                        constrainedWidth = true
                        startToEnd = iconId
                        endToStart = downloadId
                        topToTop = PARENT_ID
                        bottomToTop = descriptionId
                    }
                }
                descriptionView = TextView(ctx, null, 0, R.i.UiKit_TextView_Subtext).addTo(this) {
                    id = descriptionId
                    setLineSpacing(lineSpacingExtra, 1f)
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        constrainedWidth = true
                        startToStart = nameId
                        topToBottom = nameId
                        bottomToBottom = PARENT_ID
                    }
                }
                downloadView = ImageView(ctx).addTo(this) {
                    id = downloadId
                    setImageResource(DrawableCompat.getThemedDrawableRes(ctx, R.b.ic_file_download_opaque))
                    contentDescription = resources.getString(R.h.download)
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        endToEnd = PARENT_ID
                        topToTop = PARENT_ID
                        bottomToBottom = PARENT_ID
                    }
                }
            }
        }
    }

    // Reference: WidgetChatListAdapterItemAttachment.configureFileData
    override fun configure(component: FileMessageComponent, provider: ComponentProvider, listener: ComponentActionListener) {
        nameView.text = component.name
        descriptionView.text = Formatter.formatFileSize(context, component.size)
        iconView.setImageResource(EmbedResourceUtils.INSTANCE.getFileDrawable(component.name))

        val item = listener as WidgetChatListAdapterItemBotComponentRow
        val adapter = item.adapter

        downloadView.run {
            isEnabled = true
            alpha = 1f
            setOnClickListener {
                @SuppressLint("UseKtx")
                val url = Uri.parse(component.file.url)
                adapter.eventHandler.onQuickDownloadClicked(url, component.name)
                isEnabled = !isEnabled
                alpha = 0.3f
            }
        }
    }
}
