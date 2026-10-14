package com.aliucord.coreplugins.componentsv2.views

import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.core.graphics.ColorUtils
import com.aliucord.Logger
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.applyEmbedStyle
import com.aliucord.coreplugins.componentsv2.models.ContainerMessageComponent
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.ViewUtils.addTo
import com.aliucord.widgets.LinearLayout
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.botuikit.ComponentProvider
import com.discord.widgets.botuikit.views.ComponentActionListener
import com.discord.widgets.botuikit.views.ComponentView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRow
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRowKt
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R

class ContainerComponentView(ctx: Context) : ConstraintLayout(ctx), ComponentView<ContainerMessageComponent> {
    override fun type() = ComponentV2Type.CONTAINER

    companion object {
        private val accentDividerId = View.generateViewId()
    }

    private lateinit var accentDivider: View
    private lateinit var contentView: LinearLayout
    private lateinit var spoilerView: SpoilerView

    init {
        MaterialCardView(ctx).addTo(this) {
            applyEmbedStyle()
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                topToTop = PARENT_ID
                bottomToBottom = PARENT_ID
                startToStart = PARENT_ID
                bottomMargin = ctx.resources.getDimension(R.d.chat_cell_vertical_spacing_padding).toInt()
            }
            ConstraintLayout(ctx).addTo(this) {
                accentDivider = View(ctx).addTo(this) {
                    id = accentDividerId
                    layoutParams = LayoutParams(3.dp, 0).apply {
                        bottomToBottom = PARENT_ID
                        startToStart = PARENT_ID
                        topToTop = PARENT_ID
                    }
                }
                contentView = LinearLayout(ctx).addTo(this) {
                    setPadding(8.dp, 8.dp, 8.dp, 8.dp)
                    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        startToEnd = accentDividerId
                        endToEnd = PARENT_ID
                        topToTop = PARENT_ID
                        constrainedWidth = true
                    }
                }
                spoilerView = SpoilerView(ctx, 1).addTo(this) {
                    layoutParams = SpoilerView.constraintLayoutParamsAround(PARENT_ID)
                }
            }
        }
    }

    override fun configure(component: ContainerMessageComponent, provider: ComponentProvider, listener: ComponentActionListener) {
        val item = listener as WidgetChatListAdapterItemBotComponentRow
        val entry = item.entry
        if (entry !is BotUiComponentV2Entry) {
            Logger("ComponentsV2").warn("configured container with non-v2 entry")
            return
        }

        val configuredViews = component.components.mapIndexed { index, child ->
            provider.getConfiguredComponentView(listener, child, contentView, index)
        }.filterNotNull()
        WidgetChatListAdapterItemBotComponentRowKt.replaceViews(contentView, configuredViews)

        val color = component.accentColor?.let { ColorUtils.setAlphaComponent(it, 255) }
            ?: ColorCompat.getThemedColor(context, R.b.colorBackgroundModifierAccent)
        accentDivider.setBackgroundColor(color)

        spoilerView.configure(entry, component)
    }
}
