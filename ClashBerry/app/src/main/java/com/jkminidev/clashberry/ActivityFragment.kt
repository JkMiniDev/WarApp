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

class ActivityFragment : Fragment() {
    private var warData: WarResponse? = null
    private var rootView: FrameLayout? = null
    private var selectedSubTab: Int = 0 // 0: Attack, 1: Defence, 2: Remaining/Missed
    private var selectedClan: Int = 0 // 0: own clan, 1: opponent clan
    
    companion object {
        private const val ARG_WAR_DATA = "war_data"
        private const val ARG_SELECTED_SUB_TAB = "selected_sub_tab"
        private const val ARG_SELECTED_CLAN = "selected_clan"
        
        fun newInstance(warData: WarResponse): ActivityFragment {
            val fragment = ActivityFragment()
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
        
        // Restore selected sub-tab and clan state
        selectedSubTab = savedInstanceState?.getInt(ARG_SELECTED_SUB_TAB, 0) ?: 0
        selectedClan = savedInstanceState?.getInt(ARG_SELECTED_CLAN, 0) ?: 0
        
        if (warDataJson != null) {
            warData = Gson().fromJson(warDataJson, WarResponse::class.java)
        }
        
        val frame = FrameLayout(context)
        rootView = frame
        
        warData?.let {
            val helper = WarDisplayHelper(context)
            helper.showActivityTab(frame, it, selectedSubTab, selectedClan) { newSelectedTab, newSelectedClan ->
                selectedSubTab = newSelectedTab
                selectedClan = newSelectedClan
            }
        }
        
        return frame
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        warData?.let {
            outState.putString(ARG_WAR_DATA, Gson().toJson(it))
        }
        outState.putInt(ARG_SELECTED_SUB_TAB, selectedSubTab)
        outState.putInt(ARG_SELECTED_CLAN, selectedClan)
    }
    
    fun updateWarData(newWarData: WarResponse) {
        warData = newWarData
        // Update only the data views, not the root view, preserve selected sub-tab
        rootView?.let {
            it.removeAllViews()
            val helper = WarDisplayHelper(requireContext())
            helper.showActivityTab(it, newWarData, selectedSubTab, selectedClan) { newSelectedTab, newSelectedClan ->
                selectedSubTab = newSelectedTab
                selectedClan = newSelectedClan
            }
        }
    }
}