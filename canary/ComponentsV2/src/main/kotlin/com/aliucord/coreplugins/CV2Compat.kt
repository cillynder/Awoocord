package com.aliucord.coreplugins

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import com.aliucord.Constants
import com.aliucord.Utils
import com.aliucord.api.PatcherAPI
import com.aliucord.coreplugins.componentsv2.ComponentV2Type
import com.aliucord.coreplugins.componentsv2.ComponentsV2
import com.aliucord.coreplugins.componentsv2.isComponentV2
import com.aliucord.patcher.*
import com.aliucord.updater.ManagerBuild
import com.aliucord.utils.GsonUtils
import com.aliucord.utils.GsonUtils.toJson
import com.aliucord.utils.ReflectUtils
import com.aliucord.utils.accessField
import com.discord.api.botuikit.ComponentType
import com.discord.api.botuikit.gson.ComponentRuntimeTypeAdapter
import com.discord.api.botuikit.gson.ComponentTypeTypeAdapter
import com.discord.api.message.attachment.MessageAttachment
import com.discord.models.domain.Model
import com.discord.models.message.Message
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.google.gson.stream.JsonReader
import java.io.File
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import b.a.b.a as TypeAdapterRegistrar
import b.i.d.c as FieldNamingPolicy
import b.i.d.e as GsonBuilder

internal const val CONTENT_TYPE_GATE = "1.5.0"
internal const val EMBED_COMPONENTS_GATE = "1.5.1"

internal var MessageAttachment.contentType by gatedAccessField<String>(CONTENT_TYPE_GATE)

fun ComponentsV2.compat(patcher: PatcherAPI) {
    // check for old cursed plugin, probably not needed anymore
    val oldFile = File("${Constants.PLUGINS_PATH}/ComponentsV2-Beta.zip")
    if (oldFile.exists()) {
        logger.info("old plugin found, deleting and prompting restart")
        oldFile.delete()
        Utils.promptRestart()
        return
    }

    // I'm sorry
    // ViewRaw crashes without this
    val cuteGson = GsonBuilder().run {
        c = FieldNamingPolicy.m // LOWER_CASE_WITH_UNDERSCORES
        TypeAdapterRegistrar.a(this)
        e.add(Model.TypeAdapterFactory())
        a().apply {
            ReflectUtils.setField(this, "k", true)
        }
    }
    patcher.patch(GsonUtils::class.java.getDeclaredMethod("toJsonPretty", Object::class.java))
    { (param, obj: Any) ->
        if (obj is Message && obj.isComponentV2)
            param.result = cuteGson.toJson(obj)
    }

    // add cv2 tag
    patcher.after<WidgetChatListAdapterItemMessage>("configureItemTag", Message::class.java, Boolean::class.javaPrimitiveType!!)
    { (_, msg: Message) ->
        val textView = ReflectUtils.getField(this, "itemTag") as TextView?
            ?: return@after

        if (!msg.isComponentV2)
            return@after

        if (textView.text.isEmpty()) {
            // this code path shouldn't really ever run (only bots can send cv2, and bots have the tag already)
            // but idk maybe someone self-bots or something
            textView.visibility = View.VISIBLE
            @SuppressLint("SetTextI18n")
            textView.text = "CV2"
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        } else {
            @SuppressLint("SetTextI18n")
            textView.text = textView.text.toString() + " | CV2"
        }
    }

    ComponentV2Type.make()
    patchGson(patcher)
}

fun ComponentsV2.stopCompat() {
    unpatchGson()
    ComponentV2Type.unmake(logger)
}

private fun patchGson(patcher: PatcherAPI) {
    val factory = ComponentRuntimeTypeAdapter.INSTANCE.a()
    val typeToClass = factory.l
    val classToType = factory.m
    ComponentV2Type.newValues?.forEach {
        typeToClass[it.type.toString()] = it.clazz
        classToType[it.clazz] = it.type.toString()
    }

    patcher.instead<ComponentTypeTypeAdapter>("read", JsonReader::class.java)
    { (_, jsonReader: JsonReader) ->
        val type: Int = b.c.a.a0.d.n1(jsonReader)
        ComponentType.values().find { it.type == type } ?: ComponentType.UNKNOWN
    }
}

private fun unpatchGson() {
    val factory = ComponentRuntimeTypeAdapter.INSTANCE.a()
    val typeToClass = factory.l
    val classToType = factory.m
    ComponentV2Type.newValues?.forEach {
        typeToClass.remove(it.type.toString())
        classToType.remove(it.clazz)
    }
}

object CV2Compat {
    /** Creates a new [MessageAttachment] */
    fun createAttachment(
        filename: String,
        filesize: Long,
        proxyUrl: String,
        url: String,
        width: Int,
        height: Int,
    ): MessageAttachment {
        val inst = ReflectUtils.allocateInstance(clazz)
        filenameField.set(inst, filename)
        filesizeField.set(inst, filesize)
        proxyUrlField.set(inst, proxyUrl)
        urlField.set(inst, url)
        widthField.set(inst, width)
        heightField.set(inst, height)
        return inst
    }
}

fun <T> gatedAccessField(version: String, fieldName: String? = null): ReadWriteProperty<Any, T?> {
    return if (ManagerBuild.hasPatches(version)) {
        accessField(fieldName)
    } else {
        object : ReadWriteProperty<Any, T?> {
            override fun getValue(thisRef: Any, property: KProperty<*>): T? { return null }
            override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {}
        }
    }
}

private val clazz = MessageAttachment::class.java
private val filenameField = clazz.getDeclaredField("filename").apply { isAccessible = true }
private val filesizeField = clazz.getDeclaredField("size").apply { isAccessible = true }
private val proxyUrlField = clazz.getDeclaredField("proxyUrl").apply { isAccessible = true }
private val urlField = clazz.getDeclaredField("url").apply { isAccessible = true }
private val widthField = clazz.getDeclaredField("width").apply { isAccessible = true }
private val heightField = clazz.getDeclaredField("height").apply { isAccessible = true }
