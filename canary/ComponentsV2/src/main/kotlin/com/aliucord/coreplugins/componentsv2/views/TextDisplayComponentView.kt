package com.aliucord.coreplugins.componentsv2.views

import android.content.Context
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import com.aliucord.Logger
import com.aliucord.coreplugins.componentsv2.BotUiComponentV2Entry
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.models.TextDisplayMessageComponent
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.ViewUtils.addTo
import com.discord.stores.StoreStream
import com.discord.utilities.color.ColorCompat
import com.discord.utilities.message.MessageUtils
import com.discord.utilities.textprocessing.*
import com.discord.utilities.textprocessing.node.SpoilerNode
import com.discord.utilities.view.text.LinkifiedTextView
import com.discord.widgets.botuikit.ComponentProvider
import com.discord.widgets.botuikit.views.ComponentActionListener
import com.discord.widgets.botuikit.views.ComponentView
import com.discord.widgets.chat.list.adapter.*
import com.lytefast.flexinput.R

class TextDisplayComponentView(ctx: Context) : ConstraintLayout(ctx), ComponentView<TextDisplayMessageComponent> {
    override fun type() = ComponentV2Type.TEXT_DISPLAY

    private val textView = LinkifiedTextView(ContextThemeWrapper(ctx, R.i.UiKit_Chat_Text)).addTo(this) {
        layoutParams = LayoutParams(0, WRAP_CONTENT).apply {
            topMargin = 2.dp
            bottomMargin = 2.dp
        }
    }

    override fun configure(component: TextDisplayMessageComponent, provider: ComponentProvider, listener: ComponentActionListener) {
        val item = listener as WidgetChatListAdapterItemBotComponentRow
        val entry = item.entry
        if (entry !is BotUiComponentV2Entry) {
            Logger("ComponentsV2").warn("configured text display with non-v2 entry")
            return
        }

        render(component.id, component.content, item.adapter, entry)
    }

    private fun render(id: Int, content: String, adapter: WidgetChatListAdapter, entry: BotUiComponentV2Entry) {
        val data = adapter.data
        @Suppress("UNCHECKED_CAST")
        val spoilers = entry.state?.visibleSpoilerEmbedMap?.let {
            WidgetChatListAdapterItemEmbed.Companion.`access$getEmbedFieldVisibleIndices`(
                WidgetChatListAdapterItemEmbed.Companion,
                it,
                id,
                "comp"
            )
        } as List<Int>?
        val processor = MessagePreprocessor(entry.meId, spoilers, null, false, 50)
        val nickOrUsernames = MessageUtils.getNickOrUsernames(entry.message, entry.channel, entry.guildMembers, entry.channel.q())
        val parseChannelMessage = DiscordParser.parseChannelMessage(
            context,
            content,
            MessageRenderContext(
                context,
                entry.meId,
                false,
                nickOrUsernames,
                StoreStream.getChannels().channelNames, // TODO, does not change
                entry.guildRoles,
                R.b.colorTextLink,
                `WidgetChatListAdapterItemMessage$getMessageRenderContext$1`.INSTANCE,
                { s: String -> adapter.eventHandler.onUrlLongClicked(s) },
                ColorCompat.getThemedColor(context, R.b.theme_chat_spoiler_bg),
                ColorCompat.getThemedColor(context, R.b.theme_chat_spoiler_bg_visible),
                { node: SpoilerNode<*> -> StoreStream.getMessageState().revealSpoilerEmbedData(entry.message.id, id, "comp:${node.id}") },
                { l: Long -> adapter.eventHandler.onUserMentionClicked(l, data.channelId, data.guildId) },
                `WidgetChatListAdapterItemMessage$getMessageRenderContext$4`(context)
            ),
            processor,
            DiscordParser.ParserOptions.DEFAULT,
            false
        )
        textView.setDraweeSpanStringBuilder(parseChannelMessage);
    }
}
