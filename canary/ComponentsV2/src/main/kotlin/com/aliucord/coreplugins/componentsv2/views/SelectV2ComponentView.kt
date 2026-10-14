package com.aliucord.coreplugins.componentsv2.views

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.aliucord.Logger
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.models.SelectV2MessageComponent
import com.aliucord.coreplugins.componentsv2.selectsheet.SelectSheet
import com.aliucord.utils.ViewUtils.addTo
import com.discord.api.botuikit.ComponentType
import com.discord.models.botuikit.SelectMessageComponent
import com.discord.views.typing.TypingDots
import com.discord.widgets.botuikit.ComponentProvider
import com.discord.widgets.botuikit.views.ComponentActionListener
import com.discord.widgets.botuikit.views.ComponentView
import com.discord.widgets.botuikit.views.select.SelectComponentView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRow
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.textview.MaterialTextView

@SuppressLint("ViewConstructor")
internal class SelectV2ComponentView(context: Context, private val type: ComponentType)
    : ConstraintLayout(context), ComponentView<SelectV2MessageComponent> {
    override fun type(): ComponentType = type

    private val componentView: SelectComponentView
    private val chevron: ImageView
    private val loadingDots: TypingDots
    private val selectionIcon: SimpleDraweeView
    private val selectionText: MaterialTextView
    private val selectionsRoot: FlexboxLayout

    init {
        val view = SelectComponentView.Companion!!.inflateComponent(context, this).addTo(this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        maxWidth = view.maxWidth
        b.a.i.b5.a(view).run {
            componentView = a
            chevron = b
            loadingDots = c
            selectionIcon = d
            selectionText = e
            selectionsRoot = f
        }
    }

    override fun configure(
        component: SelectV2MessageComponent,
        provider: ComponentProvider,
        listener: ComponentActionListener,
    ) {
        val item = listener as WidgetChatListAdapterItemBotComponentRow
        val entry = item.entry
        if (entry !is BotUiComponentV2Entry) {
            Logger("ComponentsV2").warn("configured v2 select with non-v2 entry")
            return
        }

        val proxyComponent = component.run {
            SelectMessageComponent(
                type,
                index,
                stateInteraction,
                customId,
                placeholder,
                minValues,
                maxValues,
                listOf(),
                listOf(),
                emojiAnimationsEnabled,
            )
        }

        componentView.configure(proxyComponent, provider, listener)
        componentView.setOnClickListener {
            val sh = SelectSheet(entry, component)
            sh.show(item.adapter.fragmentManager, SelectSheet::class.java.name)
        }
    }
}
