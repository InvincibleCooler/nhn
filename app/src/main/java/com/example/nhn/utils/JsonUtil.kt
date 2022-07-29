package com.example.nhn.utils

import android.util.Log
import com.example.nhn.const.Constants
import org.json.JSONObject

data class ListData(
    val imagePath: String = "",
    val title: String = "",
    val desc: String = "",
    val viewType: Int = Constants.ListDataType.HEADER
) {
    var any: Any? = null
    var url: String = ""
}

object JsonUtil {
    private const val TAG = "JsonUtil"

    fun parseSummary(jsonString: String): ListData {
        val jsonObject = JSONObject(jsonString)

        var imagePath = ""
        var title = ""
        var desc = ""
        var url = ""
        var typeTitle = ""

        if (jsonObject.has("thumbnail")) {
            val thumbnail = jsonObject.getString("thumbnail")
            val json = JSONObject(thumbnail)
            if (json.has("source")) {
                imagePath = json.getString("source")
            }
        }
        if (jsonObject.has("displaytitle")) {
            title = jsonObject.getString("displaytitle")
        }
        if (jsonObject.has("extract_html")) {
            desc = jsonObject.getString("extract_html")
        }
        if (jsonObject.has("content_urls")) {
            val contentUrls = jsonObject.getString("content_urls")
            Log.d(TAG, "contentUrls : $contentUrls")
            val contentObj = JSONObject(contentUrls)
            if (contentObj.has("mobile")) {
                val mobile = JSONObject(contentObj.getString("mobile"))
                if (mobile.has("page")) {
                    url = mobile.getString("page")
                }
            }
        }
        if (jsonObject.has("title")) {
            typeTitle = jsonObject.getString("title")
        }
        Log.d(TAG, "url : $url")
        val listData = ListData(imagePath, title, desc, Constants.ListDataType.HEADER).apply {
            this.url = url
            this.any = typeTitle
        }
        Log.d(TAG, "after url : ${listData.url}")

        return listData
    }

    fun parseRelated(jsonString: String): MutableList<ListData> {
        val jsonObject = JSONObject(jsonString)

        val listData = mutableListOf<ListData>()
        if (jsonObject.has("pages")) {
            val pages = jsonObject.getJSONArray("pages")
            for (i in 0 until pages.length()) {
                val page = JSONObject(pages[i].toString())
                var imagePath = ""
                var title = ""
                var desc = ""
                if (page.has("thumbnail")) {
                    val thumbnail = page.getString("thumbnail")
                    val json = JSONObject(thumbnail)
                    if (json.has("source")) {
                        imagePath = json.getString("source")
                    }
                }
                if (page.has("title")) {
                    title = page.getString("title")
                }
                if (page.has("extract")) {
                    desc = page.getString("extract")
                }
                listData.add(ListData(imagePath, title, desc, Constants.ListDataType.ITEM))
            }
        }
        return listData
    }
}