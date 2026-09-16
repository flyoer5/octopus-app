package io.github.flyoer5.octopusapp

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.flyoer5.octopusapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())

    private val healthRunnable =
        object : Runnable {
            override fun run() {
                updateStatusUi()
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        binding.btnFont.setOnClickListener { showFontDialog() }
        applyFontSize()
        binding.btnToggle.setOnClickListener { onToggleClicked() }

        if (OctopusEngine.isAlive()) {
            binding.webView.loadUrl(WEB_URL)
        } else {
            startOctopus()
        }
        handler.post(healthRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(healthRunnable)
        super.onDestroy()
    }

    private fun showFontDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val saved = prefs.getInt(KEY_FONT_ZOOM, FONT_FOLLOW_SYSTEM)
        val systemZoom = systemFontZoom()
        val initialZoom = if (saved == FONT_FOLLOW_SYSTEM) systemZoom else saved

        val seekBar = SeekBar(this)
        seekBar.max = MAX_ZOOM - MIN_ZOOM
        seekBar.progress = initialZoom.coerceIn(MIN_ZOOM, MAX_ZOOM) - MIN_ZOOM

        val valueText = TextView(this)
        valueText.text = "$initialZoom%"
        valueText.gravity = Gravity.CENTER
        valueText.textSize = 20f

        val followSystem = CheckBox(this)
        followSystem.text = "跟随系统字体"
        followSystem.isChecked = saved == FONT_FOLLOW_SYSTEM
        followSystem.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                seekBar.progress = systemZoom - MIN_ZOOM
                valueText.text = "$systemZoom%"
            }
        }
        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    valueText.text = "${progress + MIN_ZOOM}%"
                    if (fromUser) {
                        followSystem.isChecked = false
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            },
        )

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val pad = resources.displayMetrics.density * 16
        layout.setPadding(pad.toInt(), pad.toInt(), pad.toInt(), 0)
        layout.addView(valueText)
        layout.addView(seekBar)
        layout.addView(followSystem)

        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("字体大小")
        dialog.setView(layout)
        dialog.setPositiveButton("确定") { _, _ ->
            val zoom = if (followSystem.isChecked) FONT_FOLLOW_SYSTEM else seekBar.progress + MIN_ZOOM
            prefs.edit().putInt(KEY_FONT_ZOOM, zoom).apply()
            applyFontSize()
        }
        dialog.setNegativeButton("取消", null)
        dialog.show()
    }

    private fun applyFontSize() {
        val saved = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_FONT_ZOOM, FONT_FOLLOW_SYSTEM)
        val zoom =
            if (saved == FONT_FOLLOW_SYSTEM) {
                systemFontZoom()
            } else {
                saved
            }
        binding.webView.settings.textZoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    private fun systemFontZoom(): Int = (Resources.getSystem().configuration.fontScale * 100).toInt()

    private fun setupWebView() {
        val webView = binding.webView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webView.webViewClient = WebViewClient()
    }

    private fun startOctopus() {
        requestNotificationPermissionIfNeeded()
        val ok = OctopusEngine.start(this)
        if (ok) {
            OctopusForegroundService.start(this)
            Toast.makeText(this, "正在启动 octopus...", Toast.LENGTH_SHORT).show()
            handler.postDelayed({
                binding.webView.loadUrl(WEB_URL)
            }, START_WAIT_MS)
        } else {
            Toast.makeText(this, "octopus 启动失败", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopOctopus() {
        OctopusEngine.stop()
        OctopusForegroundService.stop(this)
        Toast.makeText(this, "octopus 已停止", Toast.LENGTH_SHORT).show()
    }

    private fun onToggleClicked() {
        if (OctopusEngine.isAlive()) {
            stopOctopus()
        } else {
            startOctopus()
        }
        updateStatusUi()
    }

    private fun updateStatusUi() {
        val alive = OctopusEngine.isAlive()
        val portOpen = OctopusEngine.isPortOpen(OctopusConfig.DEFAULT_PORT)
        binding.tvStatus.text =
            when {
                alive && portOpen -> "运行中"
                alive -> "启动中"
                else -> "已停止"
            }
        val colorRes =
            if (alive && portOpen) {
                android.R.color.holo_green_dark
            } else {
                android.R.color.holo_red_dark
            }
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, colorRes))
        binding.tvPort.text = "端口 ${OctopusConfig.DEFAULT_PORT}"
        binding.btnToggle.text = if (alive) "停止服务" else "启动服务"
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val WEB_URL = "http://127.0.0.1:${OctopusConfig.DEFAULT_PORT}/"
        private const val POLL_INTERVAL_MS = 2000L
        private const val START_WAIT_MS = 1500L
        private const val PREFS_NAME = "octopus_prefs"
        private const val KEY_FONT_ZOOM = "font_zoom"
        private const val FONT_FOLLOW_SYSTEM = -1
        private const val MIN_ZOOM = 50
        private const val MAX_ZOOM = 200
    }
}
