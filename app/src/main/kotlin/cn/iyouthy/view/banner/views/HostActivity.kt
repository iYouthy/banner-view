package cn.iyouthy.view.banner.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cn.iyouthy.demo.banner.databinding.ActivityHostBinding

class HostActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityHostBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}