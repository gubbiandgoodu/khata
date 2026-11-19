package com.example.data.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object PaymentModeRegistry {

    private const val PREFS_NAME = "kharchu_payment_modes_prefs"
    private const val KEY_CUSTOM_MODES = "custom_modes_json"
    private const val KEY_DELETED_MODES = "deleted_mode_names"
    private const val KEY_MODE_OVERRIDES = "mode_overrides_json"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getAllModes(context: Context? = appContext): List<PaymentMode> {
        val ctx = context ?: appContext
        if (ctx == null) return PaymentMode.DEFAULT_MODES

        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedNames = prefs.getStringSet(KEY_DELETED_MODES, emptySet()) ?: emptySet()
        val overridesJson = prefs.getString(KEY_MODE_OVERRIDES, null)
        val overridesObj = if (!overridesJson.isNullOrBlank()) {
            try { JSONObject(overridesJson) } catch (_: Exception) { null }
        } else null

        val list = mutableListOf<PaymentMode>()

        // 1. Defaults (unless deleted, with overrides if present)
        PaymentMode.DEFAULT_MODES.forEach { def ->
            if (!deletedNames.contains(def.name)) {
                if (overridesObj != null && overridesObj.has(def.name)) {
                    val ov = overridesObj.getJSONObject(def.name)
                    list.add(
                        PaymentMode(
                            name = def.name,
                            label = ov.optString("label", def.label),
                            iconName = ov.optString("iconName", def.iconName),
                            isCustom = false,
                            colorHex = ov.optLong("colorHex", def.colorHex)
                        )
                    )
                } else {
                    list.add(def)
                }
            }
        }

        // 2. Custom modes
        val customJson = prefs.getString(KEY_CUSTOM_MODES, null)
        if (!customJson.isNullOrBlank()) {
            try {
                val array = JSONArray(customJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val name = obj.getString("name")
                    if (!deletedNames.contains(name)) {
                        list.add(
                            PaymentMode(
                                name = name,
                                label = obj.getString("label"),
                                iconName = obj.optString("iconName", "payments"),
                                isCustom = true,
                                colorHex = obj.optLong("colorHex", 0xFF3B82F6)
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (list.isEmpty()) {
            list.add(PaymentMode.CASH)
        }

        return list
    }

    fun getAllModesDirect(): List<PaymentMode> {
        return getAllModes(appContext)
    }

    fun getModeByName(name: String?): PaymentMode {
        if (name.isNullOrBlank()) return PaymentMode.CASH
        val all = getAllModes()
        return all.firstOrNull { it.name.equals(name, ignoreCase = true) || it.label.equals(name, ignoreCase = true) }
            ?: PaymentMode.DEFAULT_MODES.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: PaymentMode(name.uppercase().replace(" ", "_"), name, "payments", isCustom = true)
    }

    fun addMode(
        context: Context,
        label: String,
        iconName: String = "credit_card",
        colorHex: Long = 0xFF3B82F6
    ): PaymentMode {
        val trimmed = label.trim()
        val generatedName = "MODE_" + trimmed.uppercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' } + "_" + System.currentTimeMillis()
        val newMode = PaymentMode(
            name = generatedName,
            label = trimmed,
            iconName = iconName,
            isCustom = true,
            colorHex = colorHex
        )

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customJson = prefs.getString(KEY_CUSTOM_MODES, null)
        val array = if (!customJson.isNullOrBlank()) {
            try { JSONArray(customJson) } catch (_: Exception) { JSONArray() }
        } else {
            JSONArray()
        }

        val obj = JSONObject().apply {
            put("name", newMode.name)
            put("label", newMode.label)
            put("iconName", newMode.iconName)
            put("colorHex", newMode.colorHex)
        }
        array.put(obj)

        prefs.edit().putString(KEY_CUSTOM_MODES, array.toString()).apply()
        return newMode
    }

    fun deleteMode(context: Context, name: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedNames = prefs.getStringSet(KEY_DELETED_MODES, emptySet())?.toMutableSet() ?: mutableSetOf()
        deletedNames.add(name)
        prefs.edit().putStringSet(KEY_DELETED_MODES, deletedNames).apply()

        // Also clean custom modes
        val customJson = prefs.getString(KEY_CUSTOM_MODES, null)
        if (!customJson.isNullOrBlank()) {
            try {
                val array = JSONArray(customJson)
                val newArray = JSONArray()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("name") != name) {
                        newArray.put(obj)
                    }
                }
                prefs.edit().putString(KEY_CUSTOM_MODES, newArray.toString()).apply()
            } catch (_: Exception) {
            }
        }
        return true
    }

    fun updateMode(
        context: Context,
        name: String,
        newLabel: String,
        newIconName: String,
        newColorHex: Long
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val trimmed = newLabel.trim()

        // 1. Check if custom mode
        val customJson = prefs.getString(KEY_CUSTOM_MODES, null)
        var foundInCustom = false
        if (!customJson.isNullOrBlank()) {
            try {
                val array = JSONArray(customJson)
                val newArray = JSONArray()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("name") == name) {
                        obj.put("label", trimmed)
                        obj.put("iconName", newIconName)
                        obj.put("colorHex", newColorHex)
                        foundInCustom = true
                    }
                    newArray.put(obj)
                }
                if (foundInCustom) {
                    prefs.edit().putString(KEY_CUSTOM_MODES, newArray.toString()).apply()
                }
            } catch (_: Exception) {
            }
        }

        // 2. If default mode, store in overrides
        if (!foundInCustom) {
            val overridesJson = prefs.getString(KEY_MODE_OVERRIDES, null)
            val overridesObj = if (!overridesJson.isNullOrBlank()) {
                try { JSONObject(overridesJson) } catch (_: Exception) { JSONObject() }
            } else {
                JSONObject()
            }
            val modeObj = JSONObject().apply {
                put("label", trimmed)
                put("iconName", newIconName)
                put("colorHex", newColorHex)
            }
            overridesObj.put(name, modeObj)
            prefs.edit().putString(KEY_MODE_OVERRIDES, overridesObj.toString()).apply()
        }

        return true
    }
}
