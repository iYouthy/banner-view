### 一、已有方案分析

在项目中经常有无限轮播 Banner 的需求, 用过别人开源的, 也有自己基于 **ViewPager2** 写过类似的模块, 无限轮播据我所知的有两种思路:

1. 在原始数据的首尾各添加一个(第一个为原始数据最后一个, 最后一个为原始数据第一个), 然后轮播到末尾时偷偷切换到原始的第一个, 用户滑动到第一个后则偷偷跳到原始的最后一个;

2. 在适配器中声明数据数量为 `Int.MAX_VALUE` , 然后在一开始时切换到 `Int.MAX_VALUE / 2` , 当需要获取数据时则通过虚拟的 position 对原始数据数量取余来获取真实的数据的 index, 从而取得真实的数据.

这两种方案都能实现无限轮播, 但是实际上还是不够优雅, 例如第一个方案中, 用户在持续滑动不松手的情况下, 还是有可能够到边界(可以多加几个假数据来规避), 而第二个方案则要进行繁琐的虚拟位置映射, 调试起来也挺费心思.

那么有没有什么更加优雅的解决方案呢?

在使用过 **Paging3** 进行分页加载后, 我发现它不仅能够做到向后加载, 也能够做到向前加载, 而它提供了一个可以用在 **RecyclerView** 的适配器, 巧的是, **ViewPager2** 正是基于 **RecyclerView** 来实现的, 可以接受这个适配器!

理论存在, 那么就开始实践!

*注: 在此前未使用过 **Paging3** 的同学, 请务必前往官网了解一下这个框架, 官网提供了中文文档, 介绍得非常详细! [文档链接](https://developer.android.com/topic/libraries/architecture/paging/v3-overview?hl=zh-cn)*

### 二、制定目标

在开始编码前, 最好先定下本次编码的目标, 目标清晰了才能做到有的放矢.

在经过思考之后, 我定下了这些目标, 如果这些目标与你需要的一致, 那么也许你可以参考这个方案.

目标:

   1. 支持使用 Layout XML 配置;

   1. 能够实现无限轮播;

   1. 应该具备生命周期感知能力, 在 Resume 后开始轮播, 在 Pause 后自动停止轮播;

   1. 用户触摸 Banner 时, 应停止轮播;

   1. 使用 Paging3 为 BannerView 提供数据, 将有限的数据映射为无限;

   1. 在系统发生 **Configuration Change (配置变更)** 时, 能够保存与恢复状态(例如: 白天黑夜模式切换/ 横竖屏切换等);

   1. 能够响应数据数量变化, 动态更新 BannerItem ;

   1. 能够作为 **Item Header (头部Item)** , 嵌入 RecyclerView 中;

   1. 在目标8的基础上, 支持基于条件动态地显示/隐藏 Banner (例如数据为空时隐藏);

   1. 在目标8的基础上, 如果外部的 RecyclerView 也是基于 Paging3 , 也需要支持作为 **Item Header (头部Item)** 嵌入.


### 三、开始编码

#### 目标1: 支持使用 Layout XML 配置

为了使 BannerView 支持 Layout XML , 需要先创建一个自定义 ViewGroup , 方便起见, 我继承了 FrameLayout :

```Kotlin
class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes)
```

然后编写一个xml, 创建一个ViewPager2:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.viewpager2.widget.ViewPager2
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/vpBanner"
    android:saveEnabled="true"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:nestedScrollingEnabled="true"
    android:orientation="horizontal" />
```

将 xml 通过 `inflate` 置入到 BannerView 中, 作为它的子View:
```kotlin
private val binding: ViewBannerBinding by lazy {
    ViewBannerBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )
}
```

#### 目标2: 能够实现无限轮播

关于目标2, 我准备采用 [Kotlin Coroutins(协程)](https://kotlinlang.org/docs/coroutines-overview.html) 来实现.

无限轮播功能非常简单, 只需在协程中创建一个无限循环, 不停地切换到下一个即可, 因为即将引入 Paging3 , 所以事实上 Banner Item 的位置是能够自动对应到具体数据的.

为了能够在 xml 中配置是否启用无限轮播以及轮播时间间隔, 需要引入一些 Attribute :
```xml
./res/values/attrs

<?xml version="1.0" encoding="utf-8"?>
<resources>
    <declare-styleable name="BannerView">
        <!--是否启用自动轮播-->
        <attr name="autoSwipe" format="boolean" />
        <!--自动轮播时间间隔-->
        <attr name="swipeInterval" format="integer" />
    </declare-styleable>
</resources>
```

读取使用者的属性配置, 并提供用于代码控制的接口:
```kotlin
companion object {
    val MIN_SWIPE_INTERVAL = 1.seconds // 最小轮播时间间隔
    val DEFAULT_SWIPE_INTERVAL = 5.seconds // 默认轮播时间间隔
}

private val _autoSwipe = MutableStateFlow(true)

var autoSwipe: Boolean
    get() = _autoSwipe.value
    set(auto) = _autoSwipe.update { auto }

private val _swipeInterval = MutableStateFlow(DEFAULT_SWIPE_INTERVAL)

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
```

最后编写一个方法, 让它在合适的时机可以开始轮播:
```kotlin
private suspend fun loop(interval: Duration) {
    while (true) {
        delay(interval)
        val curr = binding.vpBanner.currentItem
        binding.vpBanner.currentItem = curr + 1
    }
}
```

#### 目标3: 应该具备生命周期感知能力, 在 Resume 后开始轮播, 在 Pause 后自动停止轮播

目标2实现了无限轮播, 但是什么时候启动它呢? 答案是在 Resume 的时候!

为了让 BannerView 能够具备生命周期感知能力, 我需要为它成为一个 LifecycleOwner 并在相关事件产生时改变生命周期状态.

*关于生命周期, 可以查看 [Jetpack Lifecycle](https://developer.android.com/topic/libraries/architecture/lifecycle?hl=zh-cn)  了解详情.*

在目标1中, 我们简单地将 BannerViw 继承了 FrameLayout ,  FrameLayout 不具备生命周期感知能力, 为了让它具有这种能力, 我需要改造一下它.

普通的View自身其实能够感知大部分的生命周期事件, 例如:

   - 构造方法: Lifecycle.Event.ON_CREATE

   -  onAttachedToWindow: Lifecycle.Event.ON_START

   - onWindowVisibilityChanged#**VISIBLE**: Lifecycle.Event.ON_RESUME

   - onWindowVisibilityChanged#**INVISIBLE**/**GONE**: Lifecycle.Event. ON_PAUSE

   - onDetachedFromWindow: Lifecycle.Event.ON_STOP

唯独 *Lifecycle.Event.ON_DESTROY* 无法自行感知, 不过管理 View 的控制器 Activity / Fragment 可以为它提供此事件, 所以我定义了一个接口(`withLifecycle`)用于控制器将 Lifecycle 传入, 用于 BannerView 同步控制器的销毁事件:
```kotlin
open class LifecycleFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), LifecycleOwner {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    init {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    @CallSuper
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    @CallSuper
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        val event = when (visibility) {
            VISIBLE -> Lifecycle.Event.ON_RESUME
            else -> Lifecycle.Event.ON_PAUSE
        }
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun withLifecycle(controllerLifecycle: Lifecycle) {
        controllerLifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
        })
    }
}
```

这里使用者需要注意的是: 如果控制器是 Activity , 则直接通过 `withLifecycle` 传入 lifecycle 即可. **但是如果控制器为 Fragment, 则需要传入 `viewLifecycleOwner.lifecycle` .**

接下来将 BannerView 的父类更改为 LIfecycleFrameLayout 即可让它也拥有生命周期感知能力:
```kotlin
class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : LifecycleFrameLayout(context, attrs, defStyleAttr, defStyleRes)
```

一旦拥有了生命周期感知能力, 我们就可以很方便地利用 `repeatOnLifecycle(Lifecycle.State.RESUMED)` 在可见的时候开始轮播, 不可见的时候停止轮播:
```kotlin
init {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
            combine(
                _autoSwipe,
                _swipeInterval,
                ::Pair
            ).collectLatest { (autoSwipe, swipeInterval) ->
                when {
                    !autoSwipe -> Unit
                    else -> loop(swipeInterval)
                }
            }
        }
    }
}
```
此处需要注意的是, **必须要使用 `collectLatest` 而不是 `collect` 来收集状态变化**, 否则无限循环不会被取消!

#### 目标4: 用户触摸 Banner 时, 应停止轮播

这个目标也很简单, 只需要在触摸事件分发到 BannerView 时, 根据 Action 来修改触摸状态:
```kotlin
private val touching = MutableStateFlow(false)

override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
    val p = parent
    require(p is ViewGroup)
    ev?.let {
        when (it.action) {
            MotionEvent.ACTION_DOWN -> touching.update { true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> touching.update { false }
        }
    }
    return super.dispatchTouchEvent(ev)
}
```

然后稍微修改目标3中控制轮播的收集源, 将 touching 状态也添加到状态源中:
```kotlin
init {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
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
```

#### 目标5: 使用 Paging3 为 BannerView 提供数据, 将有限的数据映射为无限

此处是整个功能的核心, 假设我们的原始数据的数量为4个, 我们要怎样将这些数据扩展到无限个呢?

Paging3 可以提供了根据 PageKey 来加载分页的能力, 为了实现无限的数据, 每一页我们都将原始数据稍加处理然后作为一个 Page 返回, 只要 PageKey 足够多, 那么就可以达到无限分页的效果了!

而 PageKey 可以是任何值, 只要每个分页的 key 不一样即可, 我们简单地用递增/递减的 Int 值来提供 key.

在处理数据源之前, 我们需要考虑是否可以使用原始数据, 因为 RecyclerView 会使用一个名为 `DiffUtil` 的工具来判断 Item 的差异, 而我们的列表中的数据是基于原始数据列表扩展而来, 这可能会带来一些问题, 所以我决定将原始数据包装一下, 让每个数据跟当前的页面产生一些联系从而将每个数据区分开来, 要做到这点很简单, 只要将原始数据丢进这个数据类中即可:
```kotlin
data class BannerData<T>(
    val id: String,
    val data: T,
)
```

然后提供一个比较器用于比较数据是否相同:
```kotlin
data class BannerData<T>(...) {

    class Comparator<T> : DiffUtil.ItemCallback<BannerData<T>>() {
        override fun areItemsTheSame(oldItem: BannerData<T>, newItem: BannerData<T>) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BannerData<T>, newItem: BannerData<T>) =
            oldItem == newItem
    }

}
```


我们需要为原始数据重新提供一个 id , 用于区分不同分页中的同一个数据, 这个 id 只需要与 PageKey 关联即可.

好了, 准备妥当, 接下来就是数据转换的步骤了, 我们继承 `PagingSource<Key : Any, Value : Any>` 来将数据进行转化:
```kotlin
class BannerPagingSource<Data : Any, DataId>(
    private val list: List<Data>,
    private val dataID: Data.() -> DataId
) : PagingSource<Int, BannerData<Data>>() {

    override suspend fun load(params: LoadParams<Int>) =
        when (list.isEmpty()) {
            true -> LoadResult.Error(DataEmptyException())
            false -> {
                val key = params.key ?: 0 // 起始的PageKey
                val transform = list.map { data ->
                    val id = "$key-${data.dataId()}"
                    BannerData(id, data)
                }
                LoadResult.Page(transform, key - 1, key + 1)
            }
        }
    ....
}
```

是不是非常惊讶, 竟然如此简单!

虽然代码简单, 但是我还想啰嗦地解释一下:

   1. 第一个入参 `list` 就是原始数据, 我们将每一页都视为原始数据, 所以需要持有它们.

   2. 第二个参数 `dataId` , 这是一个函数, 因为我们不清楚原始数据的 id 是什么类型的, 所以定义了一个泛型参数 `DataId` 用来泛化它, dataId 的作用是在将原始数据转化为 BannerData 的时候, 与同一分页中的其它数据做区分.

   3. 最后, 合成 BannerData 时, 提供的 id 即 `"$key-${data.dataId()}"` , 它能将每一页中相同的数据区分开来.


接下来, 就是将数据源转化为数据流, 从而提供给适配器, 这里需要你为你的控制器 (Activity / Fragment) 创建一个 ViewModel, 在里面将从上游接收到的原始数据包装为可以提供给 Paging3 适配器的数据流 :
```kotlin
class BannerVm : ViewModel() {
    private val repo = BannerRepository()

    private val sourceStateFlow = repo.bannerListFlow.scan(
        null as BannerPagingSource<Banner, Long>?
    ) { lastSource, list ->
        lastSource?.invalidate()
        BannerPagingSource(list, Banner::id)
    }.filterNotNull().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        BannerPagingSource(emptyList(), Banner::id)
    )

    val pagingDataFlow = Pager(PagingConfig(1)) {
        sourceStateFlow.value
    }.flow.cachedIn(viewModelScope)
}
```

回到 BannerView 中, 为了将数据流设置到 ViewPager2 中, 我们最好将 ViewPager2 的 Adapter 设置与获取接口对外暴露:
```kotlin
var adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>?
    get() = binding.vpBanner.adapter
    set(value) {
        binding.vpBanner.adapter = value
    }
```

接下来就是常规地创建 ViewHolder 了, 这个你可以自由发挥, 我假定这个 ViewHolder 名为 BannerContentHolder .

然后创建一个继承了 `PagingDataAdapter<T : Any, VH : RecyclerView.ViewHolder>` 的适配器:
```kotlin
class BannerContentAdapter : PagingDataAdapter<BannerData<Banner>, BannerContentHolder>(
    BannerData.Comparator()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BannerContentHolder(parent)

    override fun onBindViewHolder(holder: BannerContentHolder, position: Int) {
        val data = getItem(position)
        holder.refresh(data?.data)
    }
}
```
最后, 在合适的时机将适配器提供给 BannerView, 并且开始监听数据流
```kotlin
val adapter = BannerContentAdapter()
binding.vBanner.adapter = adapter
launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        vm.pagingDataFlow.collectLatest { pagingData ->
            adapter.submitData(pagingData)
        }
    }
}
```
至此, 一个基础的无限轮播 Banner 就已经开发完成!

我放置了几张图片在 assets 目录, 我们来预览一下效果:

![基础的BannerView](https://github.com/iYouthy/banner-view/blob/main/images/preview.png)

在自动轮播中途我尝试拖拽它一段时间, 它也确实停止了轮播, 等到我放开后它又恢复了轮播, 符合了 **目标4** 的需求.

#### 目标6: 在系统发生 **Configuration Change (配置变更)** 时, 能够保存与恢复状态(例如: 白天黑夜模式切换/ 横竖屏切换等)

目前这个 BannerView 还不完美, 每次系统配置变更时, 它都会恢复为第一页(PageKey)的第一张图片.

这是因为每次配置变更, Activity / Fragment 都被销毁重建了, 我们需要在合适的时机将 BannerView 的状态保存起来, 在重建后恢复它.

为此我们需要一个生命周期能够覆盖控制器的对象来持有这些状态, ViewModel 就是一个很好的容器!

我们之前已经为了持有 PagingDataAdapter 创建了一个 ViewModel , 这里我们直接利用它来存储状态.
```kotlin
class BannerVm : ViewModel() {
    ...

    val bannerViewState = SparseArray<Parcelable?>()

}
```

因为 BannerView 具备声明周期感知的能力, 我们直接观察它的生命周期, 在 Pause 时保存状态, 并在 Start 时尝试恢复状态:
```kotlin
binding.vBanner.lifecycle.addObserver(object : DefaultLifecycleObserver {
    override fun onPause(owner: LifecycleOwner) =
        binding.vBanner.saveHierarchyState(vm.bannerViewState)

    override fun onStart(owner: LifecycleOwner) =
        binding.vBanner.restoreHierarchyState(vm.bannerViewState)
})
```

现在, BannerView 就能在配置变更后, 保持变更前的状态了!

#### 目标7: 能够响应数据数量变化, 动态更新 BannerItem

我们在实现 **目标5** 的时候, 遵循了 MVI 模式, 所有的数据都是从上游的 Repo 提供的数据流转化而来的, 所以它自然而然能响应上游数据变化.

你可以测试一下, 一开始提供空的数据列表, 过一段时间后再提供有效的数据列表, BannerView 能够自动更新数据~

所以这个目标一不小心就被实现了, 哈哈.

#### 目标8: 能够作为 **Item Header (头部Item)** , 嵌入 RecyclerView 中

接下来进入业务领域, 在进行应用开发的时候大概率不会傻傻地放一个 Banner 在界面中, 往往都是嵌入到 RecyclerView 中的.

我们的 BannerView 能不能嵌入 RecyclerView 呢? 让我们试一下.

这块的业务代码比较多, 我就不贴上来了. 如果感兴趣可直接查看源码.

总而言之, 嵌入的 BannerView 可以工作, 但是用户无法手动拖拽它, 这就又回到那个经典的事件分发问题了, 孩子的事件被父亲拦截了.

解决的方法也很简单, 在产生 ACTION_DOWN 事件的时候请求 **父 View** 不拦截事件即可, 我们稍微修改一下之前的触摸事件分发逻辑:
```kotlin
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
        return super.dispatchTouchEvent(ev)
    }
```

*这个操作还是比较粗糙的, 如果你有更精细化的需求, 可以参考 Google 的解决方案, 但是它是用来解决 ViewPager2 内嵌 RecyclerView 的, 所以你需要稍微修改它的代码. [Google的方案(GitHub)](https://github.com/android/views-widgets-samples/blob/master/ViewPager2/app/src/main/java/androidx/viewpager2/integration/testapp/NestedScrollableHost.kt)*

#### 目标9: 在目标8的基础上, 支持基于条件动态地显示/隐藏 Banner (例如数据为空时隐藏)

现在的 BannerView 已经基本够用了, 但是在用例上还需要继续扩展: **在某些情况下需要隐藏 BannerView (如: Banner数据列表为空)**.

在这种情况下, 我们需要将数据进行一次包装, 让 Adapter 能够根据数据来识别 ViewType , 我们使用 `sealed interface` 来描述这两种类型的数据:
```kotlin
sealed interface ItemData

data object BannerItem : ItemData

data class NormalItem(val data: Int) : ItemData
```

在我们的例子中, Banner 只有一个, 我们只需要为他创建一个占位的对象, 方便区分普通类型与 Banner 类型, 我们直接用 `data object` 来声明它. 如果你有多个 Banner , 那么你需要使用 `data class` , 然后为不同的 Banner 提供一些信息, 用于绑定数据时选择不同的数据源.

区分了数据后, 就可以在 Adapter 中根据类型来处理数据了:
```kotlin
class RvAdapter(...) ... {

    override fun getItemViewType(position: Int) =
        when (getItem(position)) {
            BannerItem -> 0
            is NormalItem -> 1
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (viewType) {
            0 -> TODO("create banner holder")
            else -> TODO("create normal holder")
        }

    override fun onBindViewHolder(holder: RvHolder<*>, position: Int) {
        when (holder) {
            is BannerHolder -> Unit
            is NormalHolder -> {
                val itemData = when (val it = getItem(position)) {
                    is NormalItem -> it
                    else -> null
                }
                holder.refresh(itemData?.data)
            }
        }
    }
}
```
最后, 根据上游的提供的信息来判断是否应该展示 Banner , 合并为最终要使用的数据流:
```kotlin
combine(
    bannerVm.bannerVisibility,
    normalItemVm.dataListFlow,
    ::Pair
).map { (visibility, normalList) ->
    when (visibility) {
        false -> normalList.map { data -> NormalItem(data) }
        true -> normalList.fold(
            listOf<ItemData>(BannerItem)
        ) { banner, normalData ->
            banner + NormalItem(normalData)
        }
    }
}.collect { list ->
    rvAdapter.submitList(list) {
        val withBanner = list.any { it is BannerItem }
        if (withBanner) {
            binding.rvList.scrollToPosition(0)
        }
    }
}
```

同样的业务代码比较多, 更多细节请参考.

#### 目标10: 在目标8的基础上, 如果外部的 RecyclerView 也是基于 Paging3 , 也需要支持作为 **Item Header (头部Item)** 嵌入

终于到最后的时刻了!

这个目标就是我目前开发的项目上的实际需求了, Paging3 不愧为大厂出品, 考虑了非常多的情况, 例如: 为数据流插入头部/尾部/中间条目, 这些都是考虑周到的, 用起来非常方便.

在我们的例子中, 我们添加的是一个 Header , 所以我们在收到普通 Item 的分页数据后, 再判断一下是否需要显示 Banner , 如果需要, 则调用 `insertHeaderItem` 来添加 Header :
```kotlin
combine(
    bannerVm.bannerVisibility,
    loadMoreVm.pagingDataFlow,
    ::Pair
).collectLatest { (visible, paging) ->
    val finalPagingData = when (visible) {
        false -> paging
        true -> paging.insertHeaderItem(
            TerminalSeparatorType.SOURCE_COMPLETE,
            BannerItem
        )
    }
    rvAdapter.submitData(finalPagingData)
    if (visible) binding.rvList.scrollToPosition(0)
}
```

至此, 所有目标均已达成, 收工.

### 四、总结

Banner 作为一个非常常见的 UI 组件, 肯定是越简单高效越好, 结合了 Paging3 后, ViewPager2 完全可以实现这个目的, 而且由于用的都是官方组件, 稳定性有了非常大的保障, 后续的更新维护也不会突然停止, 好处还是很多的.

当前这个模块其实跨越了多个知识点, 包括了:
   - RecyclerView
   - ViewPager2
   - Paging3
   - ViewState
   - ViewModel
   - Lifecycle
   - Coroutines
   - Functional Programming (函数式编程)

回想几年之前, 我还在忙忙碌碌地做我的 UI 仔, 以为 Android 的开发就那么些东西, 随着学习的东西越来越多, 慢慢发现可以学的东西也越来越多, 所以所还是不要放弃学习啊!

如果这篇文章介绍的方案能够对你有所帮助, 那就太好了, 谢谢浏览到这!

### 五、补充用例

#### 2024/01/09 在仅有一个Item时, 停止自动滑动, 并禁止用户手动滑动

- 用例提供者: [@zebraoo](https://github.com/zebraoo)

- 解决方案:

   我们可以始终观察原始数据的数量, 当数量为1时, 使用 `BannerView` 提供的 `autoSwipe` 变量来禁止自动轮播:

   > [BannerVm](https://github.com/iYouthy/banner-view/blob/main/app/src/main/kotlin/cn/iyouthy/view/banner/views/BannerVm.kt)  
   > ```kotlin
   > val isSingleItem get() = repo.bannerListFlow.map { it.size == 1 }
   > ```

   > [BannerSingleItemFragment](https://github.com/iYouthy/banner-view/blob/main/app/src/main/kotlin/cn/iyouthy/view/banner/views/BannerSingleItemFragment.kt)
   > ```kotlin
   > launch {
   >     repeatOnLifecycle(Lifecycle.State.RESUMED) {
   >         bannerVm.isSingleItem.collect { single ->
   >             binding.vBanner.autoSwipe = !single
   >         }
   >     }
   > }
   > ```
  
   另外, 为了禁用用户手动滑动, 需要将 ViewPager2 的 `isUserInputEnabled` 接口暴露出来, 方便使用者控制:

   > [BannerView](https://github.com/iYouthy/banner-view/blob/main/app/src/main/kotlin/cn/iyouthy/view/banner/views/BannerView.kt)
   > ```kotlin
   >  var isUserInputEnabled
   >      get() = binding.vpBanner.isUserInputEnabled
   >      set(value) {
   >          binding.vpBanner.isUserInputEnabled = value
   >      }
   > ```

   > [BannerSingleItemFragment](https://github.com/iYouthy/banner-view/blob/main/app/src/main/kotlin/cn/iyouthy/view/banner/views/BannerSingleItemFragment.kt)
   > ```kotlin
   > launch {
   >     repeatOnLifecycle(Lifecycle.State.RESUMED) {
   >         bannerVm.isSingleItem.collect { single ->
   >             ...
   >             binding.vBanner.isUserInputEnabled = !single
   >         }
   >     }
   > }
   > ```
