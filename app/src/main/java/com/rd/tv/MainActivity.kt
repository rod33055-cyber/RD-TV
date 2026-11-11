
package com.rd.tv

import java.util.UUID
import android.app.Activity
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.widget.ImageView
import fi.iki.elonen.NanoHTTPD
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import android.text.InputType
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
    private lateinit var playerFrame: FrameLayout
    private lateinit var channelList: LinearLayout
    private lateinit var categoryBar: LinearLayout
    private lateinit var overlayCategories: LinearLayout
    private lateinit var overlayPanel: LinearLayout
    private lateinit var header: LinearLayout
    private lateinit var phonePanel: LinearLayout
    private lateinit var mainContent: LinearLayout
    private lateinit var root: LinearLayout
    private lateinit var fullButton: Button
    private lateinit var exitFullButton: Button

    private val channels = mutableListOf<Channel>()
    private var selectedCategory = "الكل"
    private var wide = false
    private var overlayVisible = false
    private var phoneFullscreen = false
    private var pairingServer: NanoHTTPD? = null
    private var pairingToken = ""

    private val categories = listOf(
        "الكل", "beIN SPORTS", "ألوان الرياضية",
        "AVA والكردية", "الفجر", "أخرى"
    )

    private val blue = Color.rgb(0, 145, 255)
    private val bg = Color.rgb(9, 13, 21)
    private val card = Color.rgb(24, 32, 45)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wide = resources.configuration.screenWidthDp >= 600

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        player = ExoPlayer.Builder(this).build()
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "تعذر تشغيل البث. تحقق من الرابط أو السيرفر",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })

        loadChannels()
        buildInterface()
    }

    private fun buildInterface() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }

        header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = TextView(this).apply {
            text = "RD-TV"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(blue)
            gravity = Gravity.CENTER_VERTICAL
        }

        val addButton = Button(this).apply {
            text = "+ إضافة"
            setTextColor(white)
            setBackgroundColor(blue)
            setOnClickListener { showChannelDialog() }
        }
       
        val qrButton = Button(this).apply {
    text = "📱 ربط الهاتف"
    setTextColor(white)
    setBackgroundColor(card)
    setOnClickListener { showPhonePairingDialog() }
        }

        fullButton = Button(this).apply {
            text = "⛶"
            textSize = 22f
            setTextColor(white)
            setBackgroundColor(card)
            setOnClickListener { togglePhoneFullscreen() }
        }

        header.addView(logo, LinearLayout.LayoutParams(0, dp(55), 1f))
        header.addView(addButton)
        header.addView(qrButton)
        header.addView(fullButton)
        root.addView(header)

        categoryBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(categoryBar)
        }

        if (!wide) root.addView(categoryScroll)

        mainContent = LinearLayout(this).apply {
            orientation = if (wide) LinearLayout.HORIZONTAL
                else LinearLayout.VERTICAL
        }

        playerFrame = FrameLayout(this)

        playerView = PlayerView(this).apply {
            player = this@MainActivity.player
            useController = true
            setBackgroundColor(Color.BLACK)
            isFocusable = true
        }

        playerFrame.addView(
            playerView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        exitFullButton = Button(this).apply {
            text = "✕ رجوع"
            setTextColor(white)
            setBackgroundColor(Color.argb(190, 0, 0, 0))
            visibility = View.GONE
            setOnClickListener { togglePhoneFullscreen() }
        }

        val exitParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dp(48),
            Gravity.TOP or Gravity.END
        )
        playerFrame.addView(exitFullButton, exitParams)

        channelList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val listTitle = TextView(this).apply {
            text = "قائمة القنوات"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            setPadding(dp(8), dp(12), dp(8), dp(10))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(channelList)
        }

        phonePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(listTitle)
            addView(
                scroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
                )
            )
        }

        if (wide) {
            mainContent.addView(
                playerFrame,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
                )
            )

            overlayPanel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = GradientDrawable().apply {
                    setColor(Color.argb(225, 9, 13, 21))
                    cornerRadius = dp(12).toFloat()
                }
                visibility = View.GONE
                isFocusable = true
            }

            val title = TextView(this).apply {
                text = "قنوات RD-TV"
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(white)
                setPadding(dp(8), dp(8), dp(8), dp(8))
            }

            overlayCategories = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val catScroll = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(overlayCategories)
            }

            val overlayScroll = ScrollView(this).apply {
                isFillViewport = true
                addView(channelList)
            }

            overlayPanel.addView(title)
            overlayPanel.addView(catScroll)
            overlayPanel.addView(
                overlayScroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
                )
            )

            playerFrame.addView(
                overlayPanel,
                FrameLayout.LayoutParams(
                    dp(390),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.END
                )
            )
        } else {
            mainContent.addView(
                playerFrame,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(220)
                )
            )
            mainContent.addView(
                phonePanel,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
                )
            )
        }

        root.addView(
            mainContent,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        setContentView(root)

        renderCategories()
        renderChannels()

        playerFrame.isFocusable = true
        playerFrame.isFocusableInTouchMode = true
        playerFrame.requestFocus()
    }

    private fun togglePhoneFullscreen() {
        if (wide) {
            toggleOverlay()
            return
        }

        phoneFullscreen = !phoneFullscreen

        header.visibility = if (phoneFullscreen) View.GONE else View.VISIBLE
        categoryBar.visibility = if (phoneFullscreen) View.GONE else View.VISIBLE
        phonePanel.visibility = if (phoneFullscreen) View.GONE else View.VISIBLE
        exitFullButton.visibility =
            if (phoneFullscreen) View.VISIBLE else View.GONE

        val params = playerFrame.layoutParams as LinearLayout.LayoutParams
        params.height = if (phoneFullscreen)
            ViewGroup.LayoutParams.MATCH_PARENT else dp(220)
        params.weight = 0f
        playerFrame.layoutParams = params

        if (phoneFullscreen) {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun toggleOverlay() {
        if (!wide) return

        overlayVisible = !overlayVisible
        overlayPanel.visibility =
            if (overlayVisible) View.VISIBLE else View.GONE

        if (overlayVisible) {
            renderCategories()
            renderChannels()
            overlayPanel.requestFocus()
        } else {
            playerFrame.requestFocus()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (wide && event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && overlayVisible) {
                toggleOverlay()
                return true
            }

            if ((event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                event.keyCode == KeyEvent.KEYCODE_ENTER) &&
                !overlayVisible &&
                (currentFocus === playerFrame ||
                 currentFocus === playerView ||
                 currentFocus == null)
            ) {
                toggleOverlay()
                return true
            }
        }

        if (!wide && phoneFullscreen &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.keyCode == KeyEvent.KEYCODE_BACK
        ) {
            togglePhoneFullscreen()
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    private fun renderCategories() {
        categoryBar.removeAllViews()

        categories.forEach { category ->
            categoryBar.addView(makeCategoryButton(category))
        }

        if (wide) {
            overlayCategories.removeAllViews()
            categories.forEach { category ->
                overlayCategories.addView(makeCategoryButton(category))
            }
        }
    }

    private fun makeCategoryButton(category: String): Button {
        return Button(this).apply {
            text = category
            textSize = 11f
            setTextColor(white)
            setBackgroundColor(
                if (selectedCategory == category) blue else card
            )
            isFocusable = true
            setOnClickListener {
                selectedCategory = category
                renderCategories()
                renderChannels()
            }
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
                text = "ماكو قنوات بهالمجموعة"
                textSize = 15f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(25), dp(12), dp(25))
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
                setPadding(dp(5), dp(4), dp(5), dp(4))
            }

            val name = TextView(this).apply {
                text = "▶  ${channel.name}\n${channel.category}"
                textSize = 15f
                setTextColor(white)
                setPadding(dp(8), dp(8), dp(4), dp(8))
                isFocusable = true
                isClickable = true
                setOnClickListener {
                    playChannel(channel)
                    if (wide && overlayVisible) toggleOverlay()
                }
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
                setTextColor(Color.RED)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { confirmDelete(index) }
            }

            row.addView(name, LinearLayout.LayoutParams(0, dp(65), 1f))
            row.addView(edit)
            row.addView(delete)

            channelList.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(6) }
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

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (index >= 0) "تعديل القناة" else "إضافة قناة")
            .setView(layout)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val n = name.text.toString().trim()
                    val u = url.text.toString().trim()

                    if (n.isEmpty() || u.isEmpty()) {
                        Toast.makeText(
                            this, "اكتب الاسم والرابط", Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    val channel = Channel(
                        n, u, spinner.selectedItem.toString()
                    )

                    if (index >= 0) channels[index] = channel
                    else channels.add(channel)

                    saveChannels()
                    selectedCategory = "الكل"
                    renderCategories()
                    renderChannels()
                    dialog.dismiss()
                }
        }

        dialog.show()
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
        } catch (_: Exception) {
            Toast.makeText(
                this, "تعذر تشغيل القناة", Toast.LENGTH_LONG
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
