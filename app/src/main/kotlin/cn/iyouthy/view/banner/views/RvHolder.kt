package cn.iyouthy.view.banner.views

import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingData
import androidx.viewbinding.ViewBinding
import cn.iyouthy.demo.banner.R
import cn.iyouthy.demo.banner.databinding.ViewItemBannerBinding
import cn.iyouthy.demo.banner.databinding.ViewItemNormalBinding
import cn.iyouthy.view.banner.abstracts.BindingHolder
import cn.iyouthy.view.banner.models.Banner
import cn.iyouthy.view.banner.models.BannerData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class RvHolder<Binding : ViewBinding>(
    parent: ViewGroup,
    @LayoutRes resId: Int,
    bind: (View) -> Binding
) : BindingHolder<Binding>(parent, resId, bind)

class BannerHolder(
    parent: ViewGroup,
    lifecycle: Lifecycle,
    bannerViewState: SparseArray<Parcelable?>,
    pagingDataFlow: Flow<PagingData<BannerData<Banner>>>
) : RvHolder<ViewItemBannerBinding>(
    parent,
    R.layout.view_item_banner,
    ViewItemBannerBinding::bind
) {

    init {
        val bannerAdapter = BannerContentAdapter(lifecycle)
        binding.vBanner.withLifecycle(lifecycle)
        binding.vBanner.adapter = bannerAdapter
        binding.vBanner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) =
                binding.vBanner.saveHierarchyState(bannerViewState)

            override fun onStart(owner: LifecycleOwner) =
                binding.vBanner.restoreHierarchyState(bannerViewState)
        })
        lifecycle.coroutineScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pagingDataFlow.collectLatest { pagingData ->
                    bannerAdapter.submitData(pagingData)
                }
            }
        }
    }
}

class NormalHolder(
    parent: ViewGroup,
    lifecycle: Lifecycle
) : RvHolder<ViewItemNormalBinding>(
    parent,
    R.layout.view_item_normal,
    ViewItemNormalBinding::bind
) {
    private val dataState = MutableStateFlow<Int?>(null)

    init {
        lifecycle.coroutineScope.launch {
            dataState.collect { data ->
                binding.tvContent.text =
                    when (data) {
                        null -> ""
                        else -> data.toString()
                    }
            }
        }
    }

    fun refresh(data: Int?) = dataState.update { data }
}