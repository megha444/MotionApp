package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvMainActivity: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvMainActivity = findViewById(R.id.tvMainActivity)

        val sdf = SimpleDateFormat("dd MMMM yy hh:mm a")
        val currentDate = sdf.format(Date())
        tvMainActivity.setText("Today: \n"+ currentDate)
    }
}