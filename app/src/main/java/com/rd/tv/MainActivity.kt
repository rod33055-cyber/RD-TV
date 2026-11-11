package com.rd.tv

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.text.InputType
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import org.json.JSONObject

data class Channel(
    val name: String,
    val url: String,
    val category: String
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
    private val backgroundColor = Color.rgb(10, 13, 20)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        player = ExoPlayer.Builder(this).build()
        loadChannels()
        buildInterface()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(14, 12, 14, 12)
        }

        val header = TextView(this).apply {
            text = "RD-TV"
            textSize = 27f
            setTextColor(blue)
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 12)
        }
        root.addView(header)

        val addButton = Button(this).apply {
            text = "+ إضافة قناة"
            setTextColor(Color.WHITE)
            setBackgroundColor(blue)
            setOnClickListener { showAddChannelDialog() }
        }
        root.addView(addButton)

        categoryBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(categoryBar)
        }
        root.addView(categoryScroll)

        playerView = PlayerView(this).apply {
            player = this@MainActivity.player
            useController = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(220)
            )
        }
        root.addView(playerView)

        val title = TextView(this).apply {
            text = "قائمة القنوات"
            textSize = 19f
            setTextColor(Color.WHITE)
            setPadding(8, 12, 8, 8)
        }
        root.addView(title)

        channelList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this)
        scroll.addView(channelList)

        root.addView(
            scroll,
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
                setTextColor(Color.WHITE)
                setBackgroundColor(
                    if (selectedCategory == category) blue
                    else Color.rgb(35, 42, 55)
                )
                setOnClickListener {
                    selectedCategory = category
                    renderCategories()
                    renderChannels()
                }
            }

            categoryBar.addView(
                button,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(48)
                )
            )
        }
    }

    private fun renderChannels() {
        channelList.removeAllViews()

        val visibleChannels = channels.filter {
            selectedCategory == "الكل" ||
            it.category == selectedCategory
        }

        visibleChannels.forEach { channel ->
            val button = Button(this).apply {
                text = "▶  ${channel.name}"
                textSize = 16f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.rgb(28, 35, 47))
                setOnClickListener {
                    playChannel(channel)
                }
            }

            channelList.addView(
                button,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(55)
                ).apply {
                    bottomMargin = dp(5)
                }
            )
        }

        if (visibleChannels.isEmpty()) {
            val empty = TextView(this).apply {
                text = "ماكو قنوات بهذي المجموعة حالياً"
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                setPadding(10, 20, 10, 20)
            }
            channelList.addView(empty)
        }
    }

    private fun showAddChannelDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 10, 20, 10)
        }

        val nameInput = EditText(this).apply {
            hint = "اسم القناة"
            setSingleLine()
        }

        val urlInput = EditText(this).apply {
            hint = "رابط البث"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_URI
        }

        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories.drop(1)
        )

        layout.addView(nameInput)
        layout.addView(urlInput)
        layout.addView(TextView(this).apply {
            text = "مجموعة القناة"
            setPadding(0, 12, 0, 4)
        })
        layout.addView(spinner)

        AlertDialog.Builder(this)
            .setTitle("إضافة قناة جديدة")
            .setView(layout)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ") { _, _ ->
                val name = nameInput.text.toString().trim()
                val url = urlInput.text.toString().trim()

                if (name.isEmpty() || url.isEmpty()) {
                    Toast.makeText(
                        this,
                        "اكتب اسم القناة والرابط",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    channels.add(
                        Channel(
                            name,
                            url,
                            spinner.selectedItem.toString()
                        )
                    )
                    saveChannels()
                    selectedCategory = "الكل"
                    renderCategories()
                    renderChannels()
                }
            }
            .show()
    }

    private fun playChannel(channel: Channel) {
        try {
            player.setMediaItem(
                MediaItem.fromUri(channel.url)
            )
            player.prepare()
            player.play()

            Toast.makeText(
                this,
                "تشغيل ${channel.name}",
                Toast.LENGTH_SHORT
            ).show()

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
            "rdtv",
            MODE_PRIVATE
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}
