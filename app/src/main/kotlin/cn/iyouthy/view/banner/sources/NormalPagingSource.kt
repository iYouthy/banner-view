package cn.iyouthy.view.banner.sources

import cn.iyouthy.view.banner.abstracts.IntPagingSource
import cn.iyouthy.view.banner.repositories.LoadMoreRepository

class NormalPagingSource(
    private val repo: LoadMoreRepository
) : IntPagingSource<Int>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
        val key = params.key ?: 0
        val prev = if (key > 0) key - 1 else null
        val next = key + 1
        val page = repo.loadWithPageKey(key)
        return LoadResult.Page(page, prev, next)
    }

}