package com.aliucord.coreplugins.componentsv2.selectsheet

import android.annotation.SuppressLint
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.constraintlayout.widget.ConstraintLayout
import com.aliucord.Utils
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.wrappers.ChannelWrapper.Companion.name
import com.aliucord.wrappers.ChannelWrapper.Companion.type
import com.aliucord.wrappers.GuildRoleWrapper.Companion.name
import com.discord.api.channel.Channel
import com.discord.models.member.GuildMember
import com.discord.utilities.color.ColorCompat
import com.discord.utilities.drawable.DrawableCompat
import com.discord.utilities.guilds.RoleUtils
import com.discord.utilities.icon.IconUtils
import com.discord.utilities.images.MGImages
import com.discord.utilities.mg_recycler.MGRecyclerViewHolder
import com.discord.utilities.user.UserUtils
import com.discord.utilities.view.extensions.ViewExtensions
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textview.MaterialTextView
import com.lytefast.flexinput.R

@SuppressLint("SetTextI18n")
internal class SelectSheetItemViewHolder(adapter: SelectSheetAdapter)
    : MGRecyclerViewHolder<SelectSheetAdapter, SelectSheetItem>(Utils.getResId("widget_select_component_bottom_sheet_item", "layout"), adapter) {

    private val description = itemView.findViewById<MaterialTextView>(Utils.getResId("select_component_sheet_item_description", "id"))!!
    private val divider = itemView.findViewById<View>(Utils.getResId("select_component_sheet_item_divider", "id"))!!
    private val dividerWithIcon = itemView.findViewById<View>(Utils.getResId("select_component_sheet_item_divider_icon", "id"))!!
    private val icon = itemView.findViewById<SimpleDraweeView>(Utils.getResId("select_component_sheet_item_icon", "id"))!!
    private val checkbox = itemView.findViewById<MaterialCheckBox>(Utils.getResId("select_component_sheet_item_selected", "id"))!!
    private val title = itemView.findViewById<MaterialTextView>(Utils.getResId("select_component_sheet_item_title", "id"))!!

    init {
        (itemView as ConstraintLayout).minHeight = 62.dp
        divider.visibility = View.GONE
        dividerWithIcon.visibility = View.VISIBLE
        dividerWithIcon.layoutParams = (dividerWithIcon.layoutParams as ConstraintLayout.LayoutParams).apply {
            marginStart = 56.dp
        }
        description.setPadding(0, 0, 0, 12.dp)
        icon.visibility = View.VISIBLE
        icon.layoutParams = (icon.layoutParams as ConstraintLayout.LayoutParams).apply {
            width = 24.dp
            height = 24.dp
        }
        MGImages.setRoundingParams(
            icon,
            Float.MAX_VALUE,
            false,
            null,
            null,
            null,
        )
    }

    override fun onConfigure(viewType: Int, item: SelectSheetItem) {
        super.onConfigure(viewType, item)

        description.visibility = View.GONE
        checkbox.visibility = if (adapter.viewModel.state?.isMultiSelect == true) VISIBLE else GONE
        checkbox.isChecked = item.checked
        title.setPadding(0, 12.dp, 0, 12.dp)

        when (item) {
            is SelectSheetItem.ChannelSelectItem -> configureChannel(item)
            is SelectSheetItem.RoleSelectItem -> configureRole(item)
            is SelectSheetItem.UserSelectItem -> configureUser(item)
        }
        itemView.setOnClickListener { adapter.viewModel.onItemSelect(item) }
        ViewExtensions.setEnabledAlpha(itemView, !item.disabled, 0.3f);
        itemView.isEnabled = !item.disabled
    }

    private fun configureChannel(item: SelectSheetItem.ChannelSelectItem) {
        title.text = "#${item.channel.name}"
        val res = when (item.channel.type) {
            Channel.GUILD_ANNOUNCEMENT -> R.e.ic_channel_announcements
            Channel.GUILD_VOICE -> R.e.ic_channel_voice
            Channel.CATEGORY -> DrawableCompat.getThemedDrawableRes(adapter.context, R.b.ic_category)
            else -> DrawableCompat.getThemedDrawableRes(adapter.context, R.b.ic_channel_text)
        }
        icon.setImageResource(res)
    }

    private fun configureRole(item: SelectSheetItem.RoleSelectItem) {
        title.text = item.role.name

        val opaqueColor: Int = RoleUtils.getOpaqueColor(item.role, ColorCompat.getColor(adapter.context, R.c.status_grey_500))
        icon.setImageResource(R.e.ic_role_24dp)
        icon.setColorFilter(opaqueColor)
    }

    private fun configureUser(item: SelectSheetItem.UserSelectItem) {
        IconUtils.`setIcon$default`(
            icon,
            item.user,
            R.d.avatar_size_standard,
            null,
            null,
            item.member,
            24,
            null
        )
        title.text = GuildMember.Companion!!.getNickOrUsername(item.member, item.user)
        val descText = UserUtils.INSTANCE.getUserNameWithDiscriminator(item.user, null, null)
        if (title.text != descText) {
            title.setPadding(0, 12.dp, 0, 0)
            description.visibility = View.VISIBLE
            description.text = descText
        }
    }
}
