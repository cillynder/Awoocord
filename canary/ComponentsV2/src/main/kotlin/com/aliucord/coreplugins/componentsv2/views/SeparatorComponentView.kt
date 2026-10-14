package com.aliucord.coreplugins.componentsv2.views

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import com.aliucord.Logger
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.models.SeparatorMessageComponent
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.ViewUtils.addTo
import com.aliucord.views.Divider
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.botuikit.ComponentProvider
import com.discord.widgets.botuikit.views.ComponentActionListener
import com.discord.widgets.botuikit.views.ComponentView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemBotComponentRow
import com.lytefast.flexinput.R

class SeparatorComponentView(ctx: Context) : ConstraintLayout(ctx), ComponentView<SeparatorMessageComponent> {
    override fun type() = ComponentV2Type.SEPARATOR

    private val divider = Divider(ctx).addTo(this) {
        setBackgroundColor(ColorCompat.getThemedColor(context, R.b.colorTextMuted));
    }

    override fun configure(component: SeparatorMessageComponent, provider: ComponentProvider, listener: ComponentActionListener) {
        val item = listener as WidgetChatListAdapterItemBotComponentRow
        val entry = item.entry
        if (entry !is BotUiComponentV2Entry) {
            Logger("ComponentsV2").warn("configured separator with non-v2 entry")
            return
        }

        divider.visibility = if (component.divider) VISIBLE else INVISIBLE
        divider.layoutParams = (divider.layoutParams as LayoutParams).apply {
            val padding = 6.dp * component.spacing
            setPadding(paddingLeft, padding, paddingRight, padding)
        }
    }
}
