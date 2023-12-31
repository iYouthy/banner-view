package cn.iyouthy.view.banner.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import cn.iyouthy.demo.banner.R
import cn.iyouthy.demo.banner.databinding.FragmentBannerOnlyBinding
import cn.iyouthy.view.banner.abstracts.BindingFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BannerOnlyFragment : BindingFragment<FragmentBannerOnlyBinding>(
    R.layout.fragment_banner_only,
    FragmentBannerOnlyBinding::bind
) {

    private val bannerVm by viewModels<BannerVm>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bannerVm.delayToLoad()
    }

    override fun onViewBindingCreated(
        scope: LifecycleCoroutineScope,
        binding: FragmentBannerOnlyBinding
    ) = scope.run {
        val bannerAdapter = BannerContentAdapter(viewLifecycleOwner.lifecycle)
        binding.vBanner.adapter = bannerAdapter
        binding.vBanner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) =
                binding.vBanner.saveHierarchyState(bannerVm.bannerViewState)

            override fun onStart(owner: LifecycleOwner) =
                binding.vBanner.restoreHierarchyState(bannerVm.bannerViewState)
        })
        launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bannerVm.pagingDataFlow.collectLatest { pagingData ->
                    bannerAdapter.submitData(pagingData)
                }
            }
        }
        launch {
            bannerAdapter.loadStateFlow.collectLatest {
                binding.vErrorState.visibility = when (it.refresh) {
                    is LoadState.Error -> View.VISIBLE
                    else -> View.GONE
                }
            }
        }
        Unit
    }

}
