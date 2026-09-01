package com.example.kidmathcontrol

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

data class MathItem(val question: String, val answer: String)

class MainActivity : AppCompatActivity() {

    private val adminPin = "1234" // PIN Admin mặc định
    private val mathList = mutableListOf<MathItem>()
    private lateinit var adapter: MathAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Kiểm tra quyền hiển thị màn hình khóa đè và khởi chạy Service đếm giờ
        checkOverlayPermission()
        startAppMonitorService()

        val layoutPin = findViewById<LinearLayout>(R.id.layoutPinSection)
        val layoutAdmin = findViewById<LinearLayout>(R.id.layoutAdminSection)
        val edtPin = findViewById<EditText>(R.id.edtPin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        val edtQuestion = findViewById<EditText>(R.id.edtQuestion)
        val edtAnswer = findViewById<EditText>(R.id.edtAnswer)
        val btnAdd = findViewById<Button>(R.id.btnAddMath)
        val rvMath = findViewById<RecyclerView>(R.id.rvMathList)

        // Load bài toán đã lưu từ trước
        loadMathData()

        adapter = MathAdapter(mathList) { position ->
            mathList.removeAt(position)
            saveMathData()
            adapter.notifyDataSetChanged()
        }
        rvMath.layoutManager = LinearLayoutManager(this)
        rvMath.adapter = adapter

        // Xử lý nút Đăng nhập Admin
        btnLogin.setOnClickListener {
            if (edtPin.text.toString() == adminPin) {
                layoutPin.visibility = View.GONE
                layoutAdmin.visibility = View.VISIBLE
                Toast.makeText(this, "Đăng nhập Admin thành công!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Mã PIN không đúng!", Toast.LENGTH_SHORT).show()
            }
        }

        // Xử lý nút Thêm bài toán mới
        btnAdd.setOnClickListener {
            val q = edtQuestion.text.toString().trim()
            val a = edtAnswer.text.toString().trim()

            if (q.isNotEmpty() && a.isNotEmpty()) {
                mathList.add(MathItem(q, a))
                saveMathData()
                adapter.notifyDataSetChanged()
                edtQuestion.text.clear()
                edtAnswer.text.clear()
                Toast.makeText(this, "Đã thêm bài toán", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Vui lòng nhập đủ đề bài và đáp án", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Hàm xin quyền "Hiển thị đè ứng dụng khác" (System Alert Window)
    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    // Hàm khởi chạy Service đếm ngầm 10 phút
    private fun startAppMonitorService() {
        val intent = Intent(this, AppMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun saveMathData() {
        val prefs = getSharedPreferences("KidMathData", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in mathList) {
            val obj = JSONObject()
            obj.put("q", item.question)
            obj.put("a", item.answer)
            jsonArray.put(obj)
        }
        prefs.edit().putString("math_list", jsonArray.toString()).apply()
    }

    private fun loadMathData() {
        val prefs = getSharedPreferences("KidMathData", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("math_list", null) ?: return
        mathList.clear()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            mathList.add(MathItem(obj.getString("q"), obj.getString("a")))
        }
    }
}

class MathAdapter(
    private val list: List<MathItem>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<MathAdapter.MathViewHolder>() {

    class MathViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestion: TextView = view.findViewById(R.id.tvQuestion)
        val tvAnswer: TextView = view.findViewById(R.id.tvAnswer)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MathViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_math, parent, false)
        return MathViewHolder(view)
    }

    override fun onBindViewHolder(holder: MathViewHolder, position: Int) {
        val item = list[position]
        holder.tvQuestion.text = item.question
        holder.tvAnswer.text = "= ${item.answer}"
        holder.btnDelete.setOnClickListener { onDeleteClick(position) }
    }

    override fun getItemCount(): Int = list.size
}
