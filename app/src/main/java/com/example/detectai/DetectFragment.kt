package com.example.detectai

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DetectFragment : Fragment(R.layout.fragment_detect) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the TabLayout and ViewPager from our new fragment_detect.xml
        val tabLayout = view.findViewById<TabLayout>(R.id.detectTabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.detectViewPager)

        // Set up the adapter to manage the Image and Text fragments
        viewPager.adapter = DetectViewPagerAdapter(childFragmentManager, lifecycle)

        // Link the TabLayout and the ViewPager together
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Image"
                1 -> "Text"
                else -> null
            }
        }.attach()
    }

    /**
     * A new, private adapter class just for our DetectFragment.
     * It will manage showing the ImageDetectionFragment and TextDetectionFragment.
     */
    private class DetectViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ImageDetectionFragment()
                1 -> TextDetectionFragment()
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }
}