package com.aliucord.coreplugins.componentsv2.models

import com.discord.api.botuikit.*
import com.discord.models.botuikit.MessageComponent

data class FileMessageComponent(
    private val type: ComponentType,
    private val index: Int,

    val id: Int,
    val file: UnfurledMediaItem,
    val spoiler: Boolean,
    val name: String,
    /** Filesize in bytes */
    val size: Long,
) : MessageComponent {
    override fun getType() = type
    override fun getIndex() = index

    companion object {
        fun mergeToMessageComponent(
            component: FileComponent,
            index: Int
        ): FileMessageComponent {
            return component.run {
                FileMessageComponent(
                    type,
                    index,
                    id,
                    file,
                    spoiler,
                    name,
                    size.toLong()
                )
            }
        }
    }
}
