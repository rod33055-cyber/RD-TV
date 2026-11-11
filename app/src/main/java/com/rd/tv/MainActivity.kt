
package com.rd.tv

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.text.InputType
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import org.json.JSONObject

data class Channel(
    var name: String,
    var url: String,
    var category: String
)

class MainActivity : Activity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var channelList: LinearLayout
    private lateinit var categoryBar: LinearLayout

    private val channels = mutableListOf<Channel>()
    private var selectedCategory = "الكل"

    private val categories = listOf(
        "الكل",
        "beIN SPORTS",
        "ألوان الرياضية",
        "AVA والكردية",
        "الفجر",
        "أخرى"
    )

    private val blue = Color.rgb(0, 145, 255)
    private val bg = Color.rgb(9, 13, 21)
    private val card = Color.rgb(24, 32, 45)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        loadChannels()
        buildInterface()
    }

    private fun buildInterface() {
        val wide = resources.configuration.screenWidthDp >= 600

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = TextView(this).apply {
            text = "RD-TV"
            textSize = 27f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(blue)
            gravity = Gravity.CENTER_VERTICAL
        }

        val addButton = Button(this).apply {
            text = "+ إضافة قناة"
            setTextColor(white)
            setBackgroundColor(blue)
            setOnClickListener { showChannelDialog() }
        }

        header.addView(
            logo,
            LinearLayout.LayoutParams(0, dp(58), 1f)
        )
        header.addView(addButton)

        root.addView(header)

        categoryBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(categoryBar)
        }

        root.addView(categoryScroll)

        val content = LinearLayout(this).apply {
            orientation = if (wide)
                LinearLayout.HORIZONTAL
            else
                LinearLayout.VERTICAL
        }

        playerView = PlayerView(this).apply {
            player = this@MainActivity.player
            useController = true
            setBackgroundColor(Color.BLACK)
        }

        channelList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val listTitle = TextView(this).apply {
            text = "قائمة القنوات"
            textSize = 19f
            setTextColor(white)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(5), dp(12), dp(5), dp(10))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(channelList)
        }

        val channelPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(listTitle)
            addView(
                scroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
        }

        if (wide) {
            content.addView(
                playerView,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 6f)
            )
            content.addView(
                channelPanel,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 4f)
            )
        } else {
            content.addView(
                playerView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(220)
                )
            )
            content.addView(
                channelPanel,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
        }

        root.addView(
            content,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        renderCategories()
        renderChannels()
    }

    private fun renderCategories() {
        categoryBar.removeAllViews()

        categories.forEach { category ->
            val button = Button(this).apply {
                text = category
                textSize = 12f
                setTextColor(white)
                setBackgroundColor(
                    if (selectedCategory == category)
                        blue else card
                )
                setOnClickListener {
                    selectedCategory = category
                    renderCategories()
                    renderChannels()
                }
            }

            categoryBar.addView(button)
        }
    }

    private fun renderChannels() {
        channelList.removeAllViews()

        val visible = channels.withIndex().filter {
            selectedCategory == "الكل" ||
                it.value.category == selectedCategory
        }

        if (visible.isEmpty()) {
            val empty = TextView(this).apply {
                text = "ماكو قنوات حالياً\nاضغط إضافة قناة حتى تبدأ"
                textSize = 15f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(30), dp(12), dp(30))
            }
            channelList.addView(empty)
        }

        visible.forEach { entry ->
            val index = entry.index
            val channel = entry.value

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(card)
                setPadding(dp(6), dp(5), dp(6), dp(5))
            }

            val name = TextView(this).apply {
                text = "▶   ${channel.name}\n${channel.category}"
                textSize = 15f
                setTextColor(white)
                setPadding(dp(8), dp(8), dp(4), dp(8))
                isFocusable = true
                setOnClickListener { playChannel(channel) }
            }

            val edit = Button(this).apply {
                text = "✎"
                setTextColor(blue)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { showChannelDialog(index) }
            }

            val delete = Button(this).apply {
                text = "×"
                textSize = 20f
                setTextColor(Color.rgb(255, 90, 90))
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { confirmDelete(index) }
            }

            row.addView(
                name,
                LinearLayout.LayoutParams(0, dp(65), 1f)
            )
            row.addView(edit)
            row.addView(delete)

            channelList.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(6)
                }
            )
        }
    }

    private fun showChannelDialog(index: Int = -1) {
        val old = if (index >= 0) channels[index] else null

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
        }

        val name = EditText(this).apply {
            hint = "اسم القناة"
            setSingleLine()
            setText(old?.name ?: "")
        }

        val url = EditText(this).apply {
            hint = "رابط البث"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_URI
            setText(old?.url ?: "")
        }

        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories.drop(1)
        )

        val oldPosition = categories.drop(1).indexOf(old?.category)
        if (oldPosition >= 0) spinner.setSelection(oldPosition)

        layout.addView(name)
        layout.addView(url)
        layout.addView(spinner)

        AlertDialog.Builder(this)
            .setTitle(if (index >= 0) "تعديل القناة" else "إضافة قناة")
            .setView(layout)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener {
                            val n = name.text.toString().trim()
                            val u = url.text.toString().trim()

                            if (n.isEmpty() || u.isEmpty()) {
                                Toast.makeText(
                                    this,
                                    "اكتب الاسم والرابط",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnClickListener
                            }

                            val channel = Channel(
                                n,
                                u,
                                spinner.selectedItem.toString()
                            )

                            if (index >= 0) {
                                channels[index] = channel
                            } else {
                                channels.add(channel)
                            }

                            saveChannels()
                            selectedCategory = "الكل"
                            renderCategories()
                            renderChannels()
                            dialog.dismiss()
                        }
                }
                dialog.show()
            }
    }

    private fun confirmDelete(index: Int) {
        AlertDialog.Builder(this)
            .setTitle("حذف القناة")
            .setMessage("متأكد تريد حذف ${channels[index].name}؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حذف") { _, _ ->
                channels.removeAt(index)
                saveChannels()
                renderChannels()
            }
            .show()
    }

    private fun playChannel(channel: Channel) {
        try {
            player.setMediaItem(MediaItem.fromUri(channel.url))
            player.prepare()
            player.play()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "تعذر تشغيل القناة",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun saveChannels() {
        val array = JSONArray()

        channels.forEach {
            val item = JSONObject()
            item.put("name", it.name)
            item.put("url", it.url)
            item.put("category", it.category)
            array.put(item)
        }

        getSharedPreferences("rdtv", MODE_PRIVATE)
            .edit()
            .putString("channels", array.toString())
            .apply()
    }

    private fun loadChannels() {
        val saved = getSharedPreferences(
            "rdtv", MODE_PRIVATE
        ).getString("channels", "[]")

        try {
            val array = JSONArray(saved)

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                channels.add(
                    Channel(
                        item.getString("name"),
                        item.getString("url"),
                        item.getString("category")
                    )
                )
            }
        } catch (_: Exception) {
            channels.clear()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}
