package cn.iyouthy.view.banner.abstracts

import androidx.paging.PagingSource
import androidx.paging.PagingState

abstract class IntPagingSource<Value : Any> : PagingSource<Int, Value>() {

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        val anchor = state.anchorPosition
        val anchorPage = anchor?.let { state.closestPageToPosition(it) }
        val prev = anchorPage?.prevKey
        val next = anchorPage?.nextKey
        val key = when {
            anchor == null -> null
            anchorPage == null -> null
            prev != null -> prev + 1
            next != null -> next - 1
            else -> null
        }
        return key
    }
}