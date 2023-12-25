package cn.iyouthy.view.banner.repositories

class LoadMoreRepository {

    fun loadWithPageKey(key: Int) =
        sequence {
            repeat(10) { index ->
                yield(key * 10 + index)
            }
        }.toList()

}