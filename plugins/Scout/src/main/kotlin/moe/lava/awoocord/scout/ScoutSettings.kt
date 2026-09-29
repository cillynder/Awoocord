package moe.lava.awoocord.scout

import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.aliucord.Constants
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.settings.delegate
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.ViewUtils.addTo
import com.aliucord.wrappers.users.globalName
import com.discord.stores.StoreStream
import com.discord.utilities.color.ColorCompat
import com.discord.views.CheckedSetting
import com.lytefast.flexinput.R
import com.lytefast.flexinput.R.h.it
import kotlin.math.roundToInt

object ScoutSettings {
    private val api = SettingsAPI("Scout")

    var persistState by api.delegate(true)

    class Page : SettingsPage() {
        override fun onViewBound(view: View) {
            super.onViewBound(view)
            setActionBarTitle("Scout")
            setPadding(0)

            val ctx = requireContext()
            linearLayout.run {
                addHeader(ctx, "Settings")

                Utils.createCheckedSetting(
                    ctx,
                    CheckedSetting.ViewType.SWITCH,
                    "Keep previous search results",
                    "Keep the previous search query and results when you reopen search in the same server",
                ).addTo(this) {
                    isChecked = persistState
                    setOnCheckedListener {
                        persistState = !persistState
                    }
                }
            }
        }
    }
}
