package cn.iyouthy.view.banner.models

class DataEmptyException
@JvmOverloads constructor(
    msg: String? = null,
    cause: Throwable? = null
) : IllegalArgumentException(msg, cause)