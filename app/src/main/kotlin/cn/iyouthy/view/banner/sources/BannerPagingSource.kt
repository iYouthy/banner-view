package cn.iyouthy.view.banner.sources

import cn.iyouthy.view.banner.abstracts.IntPagingSource
import cn.iyouthy.view.banner.models.BannerData
import cn.iyouthy.view.banner.models.DataEmptyException

class BannerPagingSource<Data : Any, DataId>(
    private val list: List<Data>,
    private val dataId: Data.() -> DataId
) : IntPagingSource<BannerData<Data>>() {

    override suspend fun load(params: LoadParams<Int>) =
        when (list.isEmpty()) {
            true -> LoadResult.Error(DataEmptyException())
            false -> {
                val key = params.key ?: 0
                val transform = list.map { data ->
                    val id = "$key-${data.dataId()}"
                    BannerData(id, data)
                }
                LoadResult.Page(transform, key - 1, key + 1)
            }
        }

}