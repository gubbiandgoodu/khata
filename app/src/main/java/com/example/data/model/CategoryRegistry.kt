package com.example.data.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object CategoryRegistry {

    private const val PREFS_NAME = "kharchu_categories_prefs"
    private const val KEY_CUSTOM_CATEGORIES = "custom_categories_json"
    private const val KEY_DELETED_CATEGORY_IDS = "deleted_category_ids"
    private const val KEY_CATEGORY_OVERRIDES = "category_overrides_json"

    fun getAllCategories(context: Context): List<CategoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedIds = prefs.getStringSet(KEY_DELETED_CATEGORY_IDS, emptySet()) ?: emptySet()
        val overridesJson = prefs.getString(KEY_CATEGORY_OVERRIDES, null)
        val overridesObj = if (!overridesJson.isNullOrBlank()) {
            try { JSONObject(overridesJson) } catch (_: Exception) { null }
        } else null

        val list = mutableListOf<CategoryItem>()

        // 1. Defaults (excluding deleted, with overrides if present)
        CategoryItem.DEFAULT_CATEGORIES.forEach { defCat ->
            if (!deletedIds.contains(defCat.id)) {
                if (overridesObj != null && overridesObj.has(defCat.id)) {
                    val ov = overridesObj.getJSONObject(defCat.id)
                    list.add(
                        CategoryItem(
                            id = defCat.id,
                            title = ov.optString("title", defCat.title),
                            colorHex = ov.optLong("colorHex", defCat.colorHex),
                            iconName = ov.optString("iconName", defCat.iconName),
                            isCustom = false
                        )
                    )
                } else {
                    list.add(defCat)
                }
            }
        }

        // 2. Custom categories
        val customJson = prefs.getString(KEY_CUSTOM_CATEGORIES, null)
        if (!customJson.isNullOrBlank()) {
            try {
                val array = JSONArray(customJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    if (!deletedIds.contains(id)) {
                        list.add(
                            CategoryItem(
                                id = id,
                                title = obj.getString("title"),
                                colorHex = obj.getLong("colorHex"),
                                iconName = obj.optString("iconName", "other"),
                                isCustom = true
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }

        // Ensure at least "Other" category is present
        if (list.isEmpty()) {
            list.add(CategoryItem.DEFAULT_CATEGORIES.last())
        }

        return list
    }

    fun getCategory(context: Context, id: String?): CategoryItem {
        if (id.isNullOrBlank()) return CategoryItem.DEFAULT_CATEGORIES.last()
        val all = getAllCategories(context)
        return resolveCategory(id, all)
    }

    fun resolveCategory(idOrTitle: String?, categories: List<CategoryItem>): CategoryItem {
        if (idOrTitle.isNullOrBlank()) {
            return categories.firstOrNull { it.id == "other" }
                ?: CategoryItem.DEFAULT_CATEGORIES.last()
        }
        val clean = idOrTitle.trim()
        return categories.firstOrNull { it.id.equals(clean, ignoreCase = true) }
            ?: categories.firstOrNull { it.title.equals(clean, ignoreCase = true) }
            ?: CategoryItem.INCOME_CATEGORIES.firstOrNull { it.id.equals(clean, ignoreCase = true) }
            ?: CategoryItem.INCOME_CATEGORIES.firstOrNull { it.title.equals(clean, ignoreCase = true) }
            ?: CategoryItem.DEFAULT_CATEGORIES.firstOrNull { it.id.equals(clean, ignoreCase = true) }
            ?: CategoryItem.DEFAULT_CATEGORIES.firstOrNull { it.title.equals(clean, ignoreCase = true) }
            ?: CategoryItem(
                id = clean,
                title = clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                colorHex = 0xFF64748B,
                iconName = "other",
                isCustom = true
            )
    }

    fun addCategory(
        context: Context,
        title: String,
        colorHex: Long,
        iconName: String
    ): CategoryItem {
        val trimmedTitle = title.trim()
        val cleanId = "cat_" + System.currentTimeMillis()
        val newItem = CategoryItem(
            id = cleanId,
            title = trimmedTitle,
            colorHex = colorHex,
            iconName = iconName,
            isCustom = true
        )

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customJson = prefs.getString(KEY_CUSTOM_CATEGORIES, null)
        val array = if (!customJson.isNullOrBlank()) {
            try { JSONArray(customJson) } catch (_: Exception) { JSONArray() }
        } else {
            JSONArray()
        }

        val obj = JSONObject().apply {
            put("id", newItem.id)
            put("title", newItem.title)
            put("colorHex", newItem.colorHex)
            put("iconName", newItem.iconName)
        }
        array.put(obj)

        prefs.edit().putString(KEY_CUSTOM_CATEGORIES, array.toString()).apply()
        return newItem
    }

    fun deleteCategory(context: Context, id: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedIds = prefs.getStringSet(KEY_DELETED_CATEGORY_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        deletedIds.add(id)
        prefs.edit().putStringSet(KEY_DELETED_CATEGORY_IDS, deletedIds).apply()

        // Also remove from custom categories if custom
        val customJson = prefs.getString(KEY_CUSTOM_CATEGORIES, null)
        if (!customJson.isNullOrBlank()) {
            try {
                val array = JSONArray(customJson)
                val newArray = JSONArray()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("id") != id) {
                        newArray.put(obj)
                    }
                }
                prefs.edit().putString(KEY_CUSTOM_CATEGORIES, newArray.toString()).apply()
            } catch (_: Exception) {
            }
        }
        return true
    }

    fun updateCategory(
        context: Context,
        id: String,
        newTitle: String,
        newColorHex: Long,
        newIconName: String
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val trimmed = newTitle.trim()

        // 1. Check if it is a custom category
        val customJson = prefs.getString(KEY_CUSTOM_CATEGORIES, null)
        var foundInCustom = false
        if (!customJson.isNullOrBlank()) {
            try {
                val array = JSONArray(customJson)
                val newArray = JSONArray()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("id") == id) {
                        obj.put("title", trimmed)
                        obj.put("colorHex", newColorHex)
                        obj.put("iconName", newIconName)
                        foundInCustom = true
                    }
                    newArray.put(obj)
                }
                if (foundInCustom) {
                    prefs.edit().putString(KEY_CUSTOM_CATEGORIES, newArray.toString()).apply()
                }
            } catch (_: Exception) {
            }
        }

        // 2. If it is a default category, store in overrides
        if (!foundInCustom) {
            val overridesJson = prefs.getString(KEY_CATEGORY_OVERRIDES, null)
            val overridesObj = if (!overridesJson.isNullOrBlank()) {
                try { JSONObject(overridesJson) } catch (_: Exception) { JSONObject() }
            } else {
                JSONObject()
            }
            val catObj = JSONObject().apply {
                put("title", trimmed)
                put("colorHex", newColorHex)
                put("iconName", newIconName)
            }
            overridesObj.put(id, catObj)
            prefs.edit().putString(KEY_CATEGORY_OVERRIDES, overridesObj.toString()).apply()
        }

        return true
    }
}
