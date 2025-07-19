package com.jkminidev.clashberry

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.jkminidev.clashberry.data.WarResponse
import com.jkminidev.clashberry.databinding.WarCardBinding
import com.jkminidev.clashberry.ui.WarDisplayHelper

class WarDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: WarCardBinding
    private lateinit var warDisplayHelper: WarDisplayHelper
    private val gson = Gson()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WarCardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        warDisplayHelper = WarDisplayHelper(this)
        
        // Get war data from intent
        val warDataJson = intent.getStringExtra("war_data")
        if (warDataJson != null) {
            val warData = gson.fromJson(warDataJson, WarResponse::class.java)
            displayWarData(warData)
        } else {
            finish()
        }
    }
    
    private fun displayWarData(warData: WarResponse) {
        // Use the existing WarDisplayHelper to show war details
        warDisplayHelper.displayWar(warData, binding.root)
        
        // Set up back button
        binding.btnBack?.setOnClickListener {
            finish()
        }
    }
}