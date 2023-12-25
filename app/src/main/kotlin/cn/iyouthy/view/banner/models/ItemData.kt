package cn.iyouthy.view.banner.models

import androidx.recyclerview.widget.DiffUtil

sealed interface ItemData {
    object Comparator : DiffUtil.ItemCallback<ItemData>() {
        override fun areItemsTheSame(oldItem: ItemData, newItem: ItemData) =
            when (oldItem) {
                BannerItem -> when (newItem) {
                    BannerItem -> true
                    is NormalItem -> false
                }

                is NormalItem -> when (newItem) {
                    BannerItem -> false
                    is NormalItem -> (oldItem == newItem)
                }
            }

        override fun areContentsTheSame(oldItem: ItemData, newItem: ItemData): Boolean {
            return when (oldItem) {
                BannerItem -> when (newItem) {
                    BannerItem -> true
                    is NormalItem -> false
                }

                is NormalItem -> when (newItem) {
                    BannerItem -> false
                    is NormalItem -> (oldItem.data == newItem.data)
                }
            }
        }
    }
}

data object BannerItem : ItemData

data class NormalItem(val data: Int) : ItemData

