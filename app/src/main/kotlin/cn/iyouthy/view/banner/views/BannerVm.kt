package cn.iyouthy.view.banner.views

import android.os.Parcelable
import android.util.SparseArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import cn.iyouthy.view.banner.models.Banner
import cn.iyouthy.view.banner.repositories.BannerRepository
import cn.iyouthy.view.banner.sources.BannerPagingSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class BannerVm : ViewModel() {
    private val repo = BannerRepository()

    val bannerVisibility = repo.bannerListFlow.map { it.isNotEmpty() }

    val isSingleItem get() = repo.bannerListFlow.map { it.size == 1 }

    val isEmpty get() = repo.bannerListFlow.map { it.isEmpty() }

    private val sourceStateFlow = repo.bannerListFlow.scan(
        null as BannerPagingSource<Banner, Long>?
    ) { lastSource, list ->
        lastSource?.invalidate()
        BannerPagingSource(list, Banner::id)
    }.filterNotNull().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        BannerPagingSource(emptyList(), Banner::id)
    )

    val pagingDataFlow = Pager(PagingConfig(1)) {
        sourceStateFlow.value
    }.flow.cachedIn(viewModelScope)

    val bannerViewState = SparseArray<Parcelable?>()

    fun delayToLoad() {
        viewModelScope.launch {
            repo.clear()
            delay(6.seconds)
            repo.recover()
        }
    }

    fun clearAndRecover() {
        viewModelScope.launch {
            delay(6.seconds)
            repo.clear()
            delay(3.seconds)
            repo.recover()
        }
    }

    fun singleAndRecover() {
        viewModelScope.launch {
            delay(6.seconds)
            repo.single()
            delay(6.seconds)
            repo.recover()
        }
    }
}