package cn.iyouthy.view.banner.abstracts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

open class BindingHolder<Binding : ViewBinding>(
    parent: ViewGroup,
    @LayoutRes resId: Int,
    bind: (View) -> Binding
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(resId, parent, false)
) {
    protected val binding: Binding by lazy { bind(itemView) }
}