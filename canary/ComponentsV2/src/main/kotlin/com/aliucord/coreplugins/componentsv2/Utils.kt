package com.aliucord.coreplugins.componentsv2

import android.content.res.ColorStateList
import android.webkit.URLUtil
import com.aliucord.coreplugins.CV2Compat
import com.aliucord.coreplugins.contentType
import com.aliucord.utils.DimenUtils.dp
import com.discord.api.botuikit.UnfurledMediaItem
import com.discord.api.message.attachment.MessageAttachment
import com.discord.utilities.color.ColorCompat
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R

fun MaterialCardView.applyEmbedStyle() {
    setCardBackgroundColor(ColorCompat.getThemedColor(context, R.b.colorBackgroundSecondary))
    radius = 8.dp.toFloat()
    elevation = 0f
    rippleColor = ColorStateList.valueOf(ColorCompat.getThemedColor(this, R.b.primary_400_alpha_30))
    strokeColor = ColorCompat.getThemedColor(this, R.b.primary_700_alpha_60)
    strokeWidth = resources.getDimensionPixelSize(R.d.chat_embed_card_stroke_width)
}

fun UnfurledMediaItem.asAttachment(
    name: String = URLUtil.guessFileName(url, null, contentType),
    size: Long = 0,
): MessageAttachment {
    return CV2Compat.createAttachment(
        name,
        size,
        proxyUrl,
        url,
        width,
        height,
    ).also { it.contentType = contentType }
}
