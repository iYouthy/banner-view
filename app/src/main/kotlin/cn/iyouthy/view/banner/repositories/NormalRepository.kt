package cn.iyouthy.view.banner.repositories

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NormalRepository {
    private val dataList = sequence {
        repeat(100) { yield(it) }
    }

    private val _dataListState by lazy {
        val list = dataList.toList()
        MutableStateFlow(list)
    }

    val dataListFlow: StateFlow<List<Int>> get() = _dataListState
}