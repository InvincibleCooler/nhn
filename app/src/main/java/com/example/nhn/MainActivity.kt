package com.example.nhn

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.nhn.activity.WebViewActivity
import com.example.nhn.const.Constants
import com.example.nhn.net.RequestManager
import com.example.nhn.utils.InputMethodUtils
import com.example.nhn.utils.JsonUtil
import com.example.nhn.utils.ListData
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        private const val TITLE_NOT_FOUNT = "Not found."
    }

    private lateinit var etSearch: EditText
    private lateinit var tvSearch: TextView
    private lateinit var listView: ListView
    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var localAdapter: LocalAdapter

    private var keyword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        keyword = intent.getStringExtra(Constants.Extra.KEYWORD) ?: ""

        etSearch = findViewById(R.id.et_search)
        tvSearch = findViewById(R.id.tv_search)
        listView = findViewById(R.id.listview)
        swipeLayout = findViewById(R.id.swipe_layout)
        progressBar = findViewById(R.id.progress_bar)

        etSearch.setText(keyword)

        localAdapter = LocalAdapter()
        listView.adapter = localAdapter

        etSearch.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    requestServer()
                    return true
                }
                return false
            }
        })

        tvSearch.setOnClickListener {
            requestServer()
        }

        swipeLayout.setOnRefreshListener {
            requestServer()
            swipeLayout.isRefreshing = false
        }

        val keyword = etSearch.text.toString().trim()
        if (keyword.isNotEmpty()) {
            requestServer()
        }
    }

    private fun requestServer() {
        InputMethodUtils.hideInputMethod(this, etSearch)

        val keyword = etSearch.text.toString().trim()
        if (keyword.isNotEmpty()) {
            loadData(keyword)
        } else {
            Toast.makeText(this, getString(R.string.input_keyword), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadData(keyword: String) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val summaryRes = requestSummaryAsync(this, keyword).await()
            val relatedRes = requestRelatedAsync(this, keyword).await()
            val summary = JsonUtil.parseSummary(summaryRes)
            val relatedList = JsonUtil.parseRelated(relatedRes)

            CoroutineScope(Dispatchers.Main).launch {
                progressBar.visibility = View.GONE
                localAdapter.items.clear()

                val type = summary.any as String
                if (type != TITLE_NOT_FOUNT) {
                    localAdapter.addItem(summary)
                    localAdapter.items = relatedList
                }
                localAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun requestSummaryAsync(scope: CoroutineScope, keyword: String): Deferred<String> =
        scope.async {
            val apiPath = "https://en.wikipedia.org/api/rest_v1/page/summary/$keyword"
            RequestManager.request(apiPath)
        }

    private fun requestRelatedAsync(scope: CoroutineScope, keyword: String): Deferred<String> =
        scope.async {
            val apiPath = "https://en.wikipedia.org/api/rest_v1/page/related/$keyword"
            RequestManager.request(apiPath)
        }


    // Adapter

    private inner class LocalAdapter : BaseAdapter() {
        var items = mutableListOf<ListData>()
            set(value) {
                field.addAll(value)
            }

        fun addItem(listData: ListData) {
            items.add(listData)
        }

        override fun getItemViewType(position: Int): Int {
            return items[position].viewType
        }

        override fun getViewTypeCount(): Int {
            return 2
        }

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(position: Int): Any {
            return items[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        @Suppress("DEPRECATION")
        override fun getView(position: Int, view: View?, parent: ViewGroup?): View? {
            val viewType = getItemViewType(position)
            var cv = view

            when (viewType) {
                Constants.ListDataType.HEADER -> { // header
                    cv = LayoutInflater.from(this@MainActivity)
                        .inflate(R.layout.listitem_main_header, parent, false)

                    val ivThumb: ImageView = cv!!.findViewById(R.id.iv_thumb)
                    val tvTitle: TextView = cv.findViewById(R.id.tv_title)
                    val tvDesc: TextView = cv.findViewById(R.id.tv_desc)

                    CoroutineScope(Dispatchers.Main).launch {
                        val bitmap = getBitmap(items[position].imagePath)
                        if (bitmap != null) {
                            ivThumb.setImageBitmap(bitmap)
                        }
                    }
                    tvTitle.text = items[position].title
                    tvDesc.text = Html.fromHtml(items[position].desc)

                    cv.setOnClickListener {
                        Log.d(TAG, "url : ${items[position].url}")
                        val intent = Intent(this@MainActivity, WebViewActivity::class.java).apply {
                            putExtra(Constants.Extra.URL, items[position].url)
                        }
                        startActivity(intent)
                    }
                }
                Constants.ListDataType.ITEM -> { // item
                    cv = LayoutInflater.from(this@MainActivity)
                        .inflate(R.layout.listitem_main_item, parent, false)

                    val ivThumb: ImageView = cv!!.findViewById(R.id.iv_thumb)
                    val tvTitle: TextView = cv.findViewById(R.id.tv_title)
                    val tvDesc: TextView = cv.findViewById(R.id.tv_desc)

                    CoroutineScope(Dispatchers.Main).launch {
                        val bitmap = getBitmap(items[position].imagePath)
                        if (bitmap != null) {
                            ivThumb.setImageBitmap(bitmap)
                        }
                    }
                    tvTitle.text = items[position].title
                    tvDesc.text = items[position].desc

                    cv.setOnClickListener {
                        val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                            putExtra(Constants.Extra.KEYWORD, items[position].title)
                        }
                        startActivity(intent)
                    }
                }
            }
            return cv
        }
    }

    suspend fun getBitmap(path: String?) = withContext(Dispatchers.IO) {
        if (path.isNullOrEmpty()) {
            return@withContext null
        }
        return@withContext try {
            val url = URL(path)
            val connection = url.openConnection() as HttpsURLConnection
            connection.doInput = true
            connection.connect()
            val `in`: InputStream = connection.inputStream
            BitmapFactory.decodeStream(`in`)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}