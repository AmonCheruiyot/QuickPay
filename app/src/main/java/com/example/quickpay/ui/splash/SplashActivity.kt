package com.example.quickpay.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.quickpay.databinding.ActivitySplashBinding
import com.example.quickpay.ui.catalog.ProductCatalogActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Smooth transition to Product Catalog after 1.8s
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, ProductCatalogActivity::class.java)
            startActivity(intent)
            finish()
        }, 1800)
    }
}
