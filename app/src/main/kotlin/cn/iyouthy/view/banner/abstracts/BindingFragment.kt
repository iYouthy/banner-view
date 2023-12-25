package cn.iyouthy.view.banner.abstracts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding

open class BindingFragment<Binding : ViewBinding>(
    @LayoutRes private val layoutId: Int,
    private val bind: (View) -> Binding
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(layoutId, container, false)

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.apply {
            val binding = bind(view)
            onViewBindingCreated(this, binding)
        }
    }

    protected open fun onViewBindingCreated(
        scope: LifecycleCoroutineScope,
        binding: Binding
    ) = Unit
}