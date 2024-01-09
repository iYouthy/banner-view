package cn.iyouthy.view.banner.repositories

import cn.iyouthy.view.banner.models.Banner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class BannerRepository {

    private val bannerList = listOf(
        Banner(1L, "file:///android_asset/images/1.png"),
        Banner(2L, "file:///android_asset/images/2.png"),
        Banner(3L, "file:///android_asset/images/3.png"),
        Banner(4L, "file:///android_asset/images/4.png"),
    )

    private val _bannerListFlow = MutableStateFlow(bannerList)
    val bannerListFlow: StateFlow<List<Banner>> = _bannerListFlow

    fun single() = _bannerListFlow.update { listOf(bannerList.first()) }

    fun clear() = _bannerListFlow.update { emptyList() }

    fun recover() = _bannerListFlow.update { bannerList }

}