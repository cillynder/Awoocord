package com.aliucord.coreplugins.componentsv2

import com.aliucord.Logger
import com.aliucord.api.PatcherAPI
import com.aliucord.coreplugins.EMBED_COMPONENTS_GATE
import com.aliucord.patcher.*
import com.aliucord.updater.ManagerBuild
import com.aliucord.utils.accessField
import com.discord.api.botuikit.Component
import com.discord.api.channel.Channel
import com.discord.api.role.GuildRole
import com.discord.models.member.GuildMember
import com.discord.models.message.Message
import com.discord.stores.StoreMessageReplies.MessageState
import com.discord.stores.StoreMessageState
import com.discord.stores.StoreThreadMessages
import com.discord.widgets.botuikit.ComponentChatListState
import com.discord.widgets.chat.list.entries.BotUiComponentEntry
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.model.WidgetChatListModelMessages

private var Message.componentsField by accessField<List<Component>?>()

fun patchMessageItems(patcher: PatcherAPI) {
    if (ManagerBuild.hasPatches(EMBED_COMPONENTS_GATE)) {
        patcher.before<ChatListEntry.Companion>(
            "createBotComponentEntries",
            Message::class.java,
            Long::class.javaPrimitiveType!!,
            ComponentChatListState.ComponentStoreState::class.java,
            Boolean::class.javaPrimitiveType!!,
        ) { (_, msg: Message) ->
            Logger("hi").debug(msg.embeds[0].components.toString())
            if (msg.embeds.any { it.isComponents }) {
                // we get a bit cheeky here :>
                msg.componentsField = msg.embeds.filter { it.isComponents }.flatMap { it.components!! }
            }
        }
        patcher.after<ChatListEntry.Companion>(
            "createBotComponentEntries",
            Message::class.java,
            Long::class.javaPrimitiveType!!,
            ComponentChatListState.ComponentStoreState::class.java,
            Boolean::class.javaPrimitiveType!!,
        ) { (_, msg: Message) ->
            if (msg.embeds.any { it.isComponents }) {
                val embedComponents = msg.embeds.flatMap { it.components!! }
                msg.componentsField = msg.componentsField?.filter { it !in embedComponents }
            }
        }
    }

    @Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY", "LocalVariableName", "UnusedVariable")
    patcher.patch(WidgetChatListModelMessages.Companion::class.java.declaredMethods.find { it.name == "getMessageItems" }!!)
    {(
         param,
         channel: Channel,
         guildMembers: Map<Long, GuildMember>,
         guildRoles: Map<Long, GuildRole>,
         _blockedRelationships: Map<Long, Int>?,
         _referencedChannel: Channel?,
         _threadStoreState: StoreThreadMessages.ThreadState?,
         _message: Message,
         state: StoreMessageState.State?,
         _repliedMessages: Map<Long, MessageState>?,
         _isBlockedExpanded: Boolean,
         _isMinimal: Boolean,
     ) ->
        @Suppress("UNCHECKED_CAST")
        val result = (param.result as MutableList<ChatListEntry>)
        val meId = param.args[15] as Long
        result.forEachIndexed { index, entry ->
            if (entry is BotUiComponentEntry && (entry.message.isComponentV2 || entry.message.embeds.any { it.isComponents })) {
                val fields = BotUiComponentV2Entry.V2Fields(state, meId, channel, guildMembers, guildRoles)
                result[index] = BotUiComponentV2Entry.fromV1(entry, fields)
            }
        }
    }
}
