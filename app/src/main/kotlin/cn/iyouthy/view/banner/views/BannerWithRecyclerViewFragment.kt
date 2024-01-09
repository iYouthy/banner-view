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
import androidx.paging.PagingData
import androidx.recyclerview.widget.ListAdapter
import cn.iyouthy.demo.banner.R
import cn.iyouthy.demo.banner.databinding.FragmentBannerWithRecyclerViewBinding
import cn.iyouthy.view.banner.abstracts.BindingFragment
import cn.iyouthy.view.banner.models.Banner
import cn.iyouthy.view.banner.models.BannerData
import cn.iyouthy.view.banner.models.BannerItem
import cn.iyouthy.view.banner.models.ItemData
import cn.iyouthy.view.banner.models.NormalItem
import cn.iyouthy.view.banner.repositories.NormalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BannerWithRecyclerViewFragment : BindingFragment<FragmentBannerWithRecyclerViewBinding>(
    R.layout.fragment_banner_with_recycler_view,
    FragmentBannerWithRecyclerViewBinding::bind
) {

    private val normalVm by viewModels<NormalVm>()
    private val bannerVm by viewModels<BannerVm>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bannerVm.clearAndRecover()
    }

    override fun onViewBindingCreated(
        scope: LifecycleCoroutineScope,
        binding: FragmentBannerWithRecyclerViewBinding
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
                    normalVm.dataListFlow,
                    ::Pair
                ).map { (visibility, normalList) ->
                    when (visibility) {
                        false -> normalList.map { data -> NormalItem(data) }
                        true -> normalList.fold(
                            listOf<ItemData>(BannerItem)
                        ) { banner, normalData ->
                            banner + NormalItem(normalData)
                        }
                    }
                }.collectLatest { list ->
                    rvAdapter.submitList(list) {
                        val withBanner = list.any { it is BannerItem }
                        if (withBanner) {
                            binding.rvList.scrollToPosition(0)
                        }
                    }
                }
            }
        }
        Unit
    }

    private class RvAdapter(
        private val lifecycle: Lifecycle,
        private val bannerViewState: SparseArray<Parcelable?>,
        private val pagingDataFlow: Flow<PagingData<BannerData<Banner>>>
    ) : ListAdapter<ItemData, RvHolder<*>>(ItemData.Comparator) {
        override fun getItemViewType(position: Int) =
            when (getItem(position)) {
                BannerItem -> 0
                is NormalItem -> 1
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

    class NormalVm : ViewModel() {
        private val repo = NormalRepository()

        val dataListFlow get() = repo.dataListFlow
    }
}
