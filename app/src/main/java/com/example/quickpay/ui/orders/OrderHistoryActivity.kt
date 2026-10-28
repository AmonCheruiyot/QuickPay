package com.example.quickpay.ui.orders

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quickpay.data.repository.EcommerceRepository
import com.example.quickpay.databinding.ActivityOrderHistoryBinding
import com.example.quickpay.utils.UserSessionManager
import kotlinx.coroutines.launch

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderHistoryBinding
    private lateinit var adapter: OrderAdapter
    private val repository = EcommerceRepository()
    private lateinit var sessionManager: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = UserSessionManager(this)

        setupRecyclerView()
        loadOrderHistory()
    }

    private fun setupRecyclerView() {
        adapter = OrderAdapter { order ->
            val intent = Intent(this, OrderDetailsActivity::class.java).apply {
                putExtra("EXTRA_ORDER_REF", order.orderReference)
            }
            startActivity(intent)
        }

        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = adapter
    }

    private fun loadOrderHistory() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyOrders.visibility = View.GONE
        binding.rvOrders.visibility = View.GONE

        lifecycleScope.launch {
            val user = sessionManager.getUser()
            val result = repository.getOrderHistory(userId = user?.id)
            binding.progressBar.visibility = View.GONE

            result.fold(
                onSuccess = { orders ->
                    if (orders.isEmpty()) {
                        binding.tvEmptyOrders.visibility = View.VISIBLE
                        binding.rvOrders.visibility = View.GONE
                    } else {
                        binding.tvEmptyOrders.visibility = View.GONE
                        binding.rvOrders.visibility = View.VISIBLE
                        adapter.updateList(orders)
                    }
                },
                onFailure = { error ->
                    binding.tvEmptyOrders.visibility = View.VISIBLE
                    binding.tvEmptyOrders.text = error.message ?: "Failed to load orders"
                    Toast.makeText(this@OrderHistoryActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
