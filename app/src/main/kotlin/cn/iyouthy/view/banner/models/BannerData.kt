package cn.iyouthy.view.banner.models

import androidx.recyclerview.widget.DiffUtil

/** 包装后的数据. */
data class BannerData<T>(
    val id: String,
    val data: T,
) {

    class Comparator<T> : DiffUtil.ItemCallback<BannerData<T>>() {
        override fun areItemsTheSame(oldItem: BannerData<T>, newItem: BannerData<T>) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BannerData<T>, newItem: BannerData<T>) =
            oldItem == newItem
    }
}