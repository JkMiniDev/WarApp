package com.jkminidev.clashberry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.jkminidev.clashberry.data.WarResponse
import com.jkminidev.clashberry.ui.WarDisplayHelper

class OverviewFragment : Fragment() {
    private var warData: WarResponse? = null
    private var rootView: FrameLayout? = null
    
    companion object {
        private const val ARG_WAR_DATA = "war_data"
        
        fun newInstance(warData: WarResponse): OverviewFragment {
            val fragment = OverviewFragment()
            val args = Bundle()
            args.putString(ARG_WAR_DATA, Gson().toJson(warData))
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = requireContext()
        
        // Restore war data from arguments or saved state
        val warDataJson = savedInstanceState?.getString(ARG_WAR_DATA) 
            ?: arguments?.getString(ARG_WAR_DATA)
        
        if (warDataJson != null) {
            warData = Gson().fromJson(warDataJson, WarResponse::class.java)
        }
        
        val frame = FrameLayout(context)
        rootView = frame
        
        warData?.let { 
            val helper = WarDisplayHelper(context)
            helper.displayWar(it, frame, com.google.android.material.tabs.TabLayout(context)) 
        }
        
        return frame
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        warData?.let {
            outState.putString(ARG_WAR_DATA, Gson().toJson(it))
        }
    }
    
    fun updateWarData(newWarData: WarResponse) {
        warData = newWarData
        // Update only the data views, not the root view
        rootView?.let {
            it.removeAllViews()
            val helper = WarDisplayHelper(requireContext())
            helper.displayWar(newWarData, it, com.google.android.material.tabs.TabLayout(requireContext()))
        }
    }
}