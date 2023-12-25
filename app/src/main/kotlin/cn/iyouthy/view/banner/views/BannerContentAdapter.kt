package cn.iyouthy.view.banner.views

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.paging.PagingDataAdapter
import cn.iyouthy.view.banner.models.Banner
import cn.iyouthy.view.banner.models.BannerData

class BannerContentAdapter(
    private val lifecycle: Lifecycle
) : PagingDataAdapter<BannerData<Banner>, BannerContentHolder>(
    BannerData.Comparator()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BannerContentHolder(parent, lifecycle)

    override fun onBindViewHolder(holder: BannerContentHolder, position: Int) {
        val data = getItem(position)
        holder.refresh(data?.data)
    }
}