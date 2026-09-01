package com.example.kidmathcontrol

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import kotlin.random.Random

class AppMonitorService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Thời gian đếm ngược: 10 * 60 * 1000L (10 phút). 
    // Đang để 10.000L (10 giây) để bạn tiện test thử APK!
    private val intervalMillis = 10000L 
    private var currentAnswer = ""

    private val timerRunnable = object : Runnable {
        override fun run() {
            showLockOverlay()
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundService()
        resetTimer()
    }

    private fun resetTimer() {
        handler.removeCallbacks(timerRunnable)
        handler.postDelayed(timerRunnable, intervalMillis)
    }

    private fun showLockOverlay() {
        if (overlayView != null) return // Đã hiển thị màn hình khóa rồi

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.layout_lock_overlay, null)

        val tvQuestion = overlayView?.findViewById<TextView>(R.id.tvLockQuestion)
        val edtAnswer = overlayView?.findViewById<EditText>(R.id.edtLockAnswer)
        val btnSubmit = overlayView?.findViewById<Button>(R.btnSubmitAnswer)

        // Lấy ngẫu nhiên 1 bài toán từ danh sách Admin đã lưu
        val math = getRandomMath()
        tvQuestion?.text = "${math.first} = ?"
        currentAnswer = math.second

        btnSubmit?.setOnClickListener {
            val userAns = edtAnswer?.text.toString().trim()
            if (userAns == currentAnswer) {
                Toast.makeText(this, "Chính xác! Bé được dùng tiếp.", Toast.LENGTH_SHORT).show()
                removeOverlay()
                resetTimer() // Reset đếm lại từ đầu
            } else {
                Toast.makeText(this, "Kết quả chưa đúng, hãy thử lại!", Toast.LENGTH_SHORT).show()
            }
        }

        windowManager.addView(overlayView, params)
    }

    private fun removeOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }
    }

    private fun getRandomMath(): Pair<String, String> {
        val prefs = getSharedPreferences("KidMathData", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("math_list", null)
        if (!jsonString.isNullOrEmpty()) {
            val jsonArray = JSONArray(jsonString)
            if (jsonArray.length() > 0) {
                val randomIndex = Random.nextInt(jsonArray.length())
                val obj = jsonArray.getJSONObject(randomIndex)
                return Pair(obj.getString("q"), obj.getString("a"))
            }
        }
        // Mặc định nếu chưa có bài toán nào
        return Pair("5 + 5", "10")
    }

    private fun startForegroundService() {
        val channelId = "KidMathServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Kid Math Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Kid Math Control đang chạy")
            .setContentText("Giám sát thời gian sử dụng thiết bị")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(timerRunnable)
        removeOverlay()
        super.onDestroy()
    }
}
