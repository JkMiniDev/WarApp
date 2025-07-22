package com.jkminidev.clashberry

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.jkminidev.clashberry.data.WarResponse
import com.jkminidev.clashberry.databinding.ActivityWarDetailBinding
import com.jkminidev.clashberry.ui.WarDisplayHelper

class WarDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWarDetailBinding
    private lateinit var warDisplayHelper: WarDisplayHelper
    private val gson = Gson()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        warDisplayHelper = WarDisplayHelper(this)
        
        // Set up toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
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
        warDisplayHelper.displayWar(warData, binding.warContentContainer, binding.tabLayout)
    }
}