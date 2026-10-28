package com.example.quickpay.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.quickpay.data.repository.EcommerceRepository
import com.example.quickpay.databinding.ActivityAdminDashboardBinding
import com.example.quickpay.ui.catalog.ProductCatalogActivity
import com.example.quickpay.ui.orders.OrderHistoryActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val repository = EcommerceRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        loadDashboardStats()
    }

    private fun setupListeners() {
        binding.btnViewAllOrders.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        binding.btnBackStore.setOnClickListener {
            startActivity(Intent(this, ProductCatalogActivity::class.java))
            finish()
        }
    }

    private fun loadDashboardStats() {
        lifecycleScope.launch {
            val result = repository.getAdminDashboard()
            result.fold(
                onSuccess = { stats ->
                    val formatter = NumberFormat.getNumberInstance(Locale.US)
                    binding.tvTotalRevenue.text = "KES ${formatter.format(stats.totalRevenue)}"
                    binding.tvTotalOrders.text = stats.totalOrders.toString()
                    binding.tvPendingOrders.text = stats.pendingOrders.toString()
                    binding.tvLowStockCount.text = stats.lowStockCount.toString()
                },
                onFailure = { error ->
                    Toast.makeText(this@AdminDashboardActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
