package cn.iyouthy.view.banner.views

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.paging.TerminalSeparatorType
import androidx.paging.cachedIn
import androidx.paging.insertHeaderItem
import androidx.paging.map
import cn.iyouthy.demo.banner.R
import cn.iyouthy.demo.banner.databinding.FragmentBannerWithPagingRecyclerViewBinding
import cn.iyouthy.view.banner.abstracts.BindingFragment
import cn.iyouthy.view.banner.models.Banner
import cn.iyouthy.view.banner.models.BannerData
import cn.iyouthy.view.banner.models.BannerItem
import cn.iyouthy.view.banner.models.ItemData
import cn.iyouthy.view.banner.models.NormalItem
import cn.iyouthy.view.banner.repositories.LoadMoreRepository
import cn.iyouthy.view.banner.sources.NormalPagingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BannerWithPagingRecyclerViewFragment
    : BindingFragment<FragmentBannerWithPagingRecyclerViewBinding>(
    R.layout.fragment_banner_with_paging_recycler_view,
    FragmentBannerWithPagingRecyclerViewBinding::bind
) {

    private val loadMoreVm by viewModels<LoadMoreVm>()
    private val bannerVm by viewModels<BannerVm>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bannerVm.clearAndRecover()
    }

    override fun onViewBindingCreated(
        scope: LifecycleCoroutineScope,
        binding: FragmentBannerWithPagingRecyclerViewBinding
    ) = scope.run {

        val rvAdapter = RvAdapter(
            viewLifecycleOwner.lifecycle,
            bannerVm.bannerViewState,
            bannerVm.pagingDataFlow
        )
        binding.rvList.adapter = rvAdapter

        launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    bannerVm.bannerVisibility,
                    loadMoreVm.pagingDataFlow,
                    ::Pair
                ).collectLatest { (visible, paging) ->
                    val finalPagingData = when (visible) {
                        false -> paging
                        true -> paging.insertHeaderItem(
                            TerminalSeparatorType.SOURCE_COMPLETE,
                            BannerItem
                        )
                    }
                    launch { rvAdapter.submitData(finalPagingData) }
                    if (visible) binding.rvList.scrollToPosition(0)
                }
            }
        }
        Unit
    }

    private class RvAdapter(
        private val lifecycle: Lifecycle,
        private val bannerViewState: SparseArray<Parcelable?>,
        private val pagingDataFlow: Flow<PagingData<BannerData<Banner>>>
    ) : PagingDataAdapter<ItemData, RvHolder<*>>(ItemData.Comparator) {
        override fun getItemViewType(position: Int) =
            when (getItem(position)) {
                BannerItem -> 0
                else -> 1
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            when (viewType) {
                0 -> BannerHolder(parent, lifecycle, bannerViewState, pagingDataFlow)
                else -> NormalHolder(parent, lifecycle)
            }

        override fun onBindViewHolder(holder: RvHolder<*>, position: Int) {
            when (holder) {
                is BannerHolder -> Unit
                is NormalHolder -> {
                    val itemData = when (val it = getItem(position)) {
                        is NormalItem -> it
                        else -> null
                    }
                    holder.refresh(itemData?.data)
                }
            }
        }
    }


    class LoadMoreVm : ViewModel() {
        private val repo = LoadMoreRepository()

        val pagingDataFlow: Flow<PagingData<ItemData>> = Pager(PagingConfig(10)) {
            NormalPagingSource(repo)
        }.flow.cachedIn(viewModelScope).map {
            it.map { data -> NormalItem(data) }
        }

    }
}