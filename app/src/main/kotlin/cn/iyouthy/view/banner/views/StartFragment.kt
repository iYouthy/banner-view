package cn.iyouthy.view.banner.views

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.fragment.findNavController
import cn.iyouthy.demo.banner.R
import cn.iyouthy.demo.banner.databinding.FragmentStartBinding
import cn.iyouthy.view.banner.abstracts.BindingFragment

class StartFragment : BindingFragment<FragmentStartBinding>(
    R.layout.fragment_start,
    FragmentStartBinding::bind
) {

    override fun onViewBindingCreated(
        scope: LifecycleCoroutineScope,
        binding: FragmentStartBinding
    ) {
        binding.btnBannerOnly.setOnClickListener {
            findNavController().navigate(R.id.toBannerOnlyFragment)
        }
        binding.btnBannerWithRecyclerView.setOnClickListener {
            findNavController().navigate(R.id.toBannerWithRecyclerViewFragment)
        }
        binding.btnBannerWithPagingRecyclerView.setOnClickListener {
            findNavController().navigate(R.id.toBannerWithPagingRecyclerViewFragment)
        }
    }

}