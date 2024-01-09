package cn.iyouthy.view.banner.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import cn.iyouthy.demo.banner.R
import cn.iyouthy.demo.banner.databinding.ViewBannerBinding
import cn.iyouthy.view.banner.abstracts.LifecycleFrameLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : LifecycleFrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    companion object {
        val MIN_SWIPE_INTERVAL = 1.seconds
        val DEFAULT_SWIPE_INTERVAL = 5.seconds
    }

    private val binding: ViewBannerBinding by lazy {
        ViewBannerBinding.inflate(LayoutInflater.from(context), this, true)
    }

    private val touching = MutableStateFlow(false)

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val p = parent
        require(p is ViewGroup)
        ev?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    p.requestDisallowInterceptTouchEvent(true)
                    touching.update { true }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    p.requestDisallowInterceptTouchEvent(false)
                    touching.update { false }
                }
            }
        }
        val isDownAction = ev?.action == MotionEvent.ACTION_DOWN
        val handled = super.dispatchTouchEvent(ev)
        if (isDownAction && !handled) touching.update { false }
        return handled
    }

    private val _autoSwipe = MutableStateFlow(true)

    @Suppress("unused")
    var autoSwipe: Boolean
        get() = _autoSwipe.value
        set(auto) = _autoSwipe.update { auto }

    private val _swipeInterval = MutableStateFlow(DEFAULT_SWIPE_INTERVAL)

    @Suppress("unused")
    var swipeInterval: Duration
        get() = _swipeInterval.value
        set(duration) = _swipeInterval.update { duration }

    init {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.BannerView
        ).let { ta ->
            // 自动滑动开关
            ta.getBoolean(
                R.styleable.BannerView_autoSwipe,
                true
            ).also { enable ->
                _autoSwipe.update { enable }
            }

            // 自动滑动间隔
            ta.getInt(
                R.styleable.BannerView_swipeInterval,
                DEFAULT_SWIPE_INTERVAL.inWholeMilliseconds.toInt()
            ).milliseconds.also { duration ->
                _swipeInterval.update {
                    when {
                        duration > MIN_SWIPE_INTERVAL -> duration
                        else -> MIN_SWIPE_INTERVAL
                    }
                }
            }
            ta.recycle()
        }
    }

    private suspend fun loop(interval: Duration) {
        while (true) {
            delay(interval)
            val curr = binding.vpBanner.currentItem
            binding.vpBanner.currentItem = curr + 1
        }
    }

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                combine(
                    touching,
                    _autoSwipe,
                    _swipeInterval,
                    ::Triple
                ).collectLatest { (touching, autoSwipe, swipeInterval) ->
                    when {
                        touching -> Unit
                        !autoSwipe -> Unit
                        else -> loop(swipeInterval)
                    }
                }
            }
        }
    }

    var adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>?
        get() = binding.vpBanner.adapter
        set(value) {
            binding.vpBanner.adapter = value
        }

    var isUserInputEnabled
        get() = binding.vpBanner.isUserInputEnabled
        set(value) {
            binding.vpBanner.isUserInputEnabled = value
        }

}
