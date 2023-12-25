package cn.iyouthy.view.banner.views

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import cn.iyouthy.demo.banner.R
import cn.iyouthy.demo.banner.databinding.ViewItemBannerContentBinding
import cn.iyouthy.view.banner.abstracts.BindingHolder
import cn.iyouthy.view.banner.models.Banner
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BannerContentHolder(
    parent: ViewGroup,
    lifecycle: Lifecycle,
) : BindingHolder<ViewItemBannerContentBinding>(
    parent,
    R.layout.view_item_banner_content,
    ViewItemBannerContentBinding::bind
) {

    private val dataState = MutableStateFlow<Banner?>(null)

    init {
        lifecycle.coroutineScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataState.collect { data ->
                    when (data) {
                        null -> binding.ivBanner.visibility = View.GONE
                        else -> {
                            binding.ivBanner.visibility = View.VISIBLE
                            Glide.with(binding.ivBanner)
                                .load(data.imageUri)
                                .into(binding.ivBanner)
                        }
                    }
                }
            }
        }
    }

    fun refresh(data: Banner?) = dataState.update { data }
}