package com.example.quickpay.ui.orders

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.quickpay.R
import com.example.quickpay.data.model.Order
import com.example.quickpay.data.repository.EcommerceRepository
import com.example.quickpay.databinding.ActivityOrderDetailsBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class OrderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailsBinding
    private val repository = EcommerceRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ref = intent.getStringExtra("EXTRA_ORDER_REF")
        if (ref.isNullOrEmpty()) {
            Toast.makeText(this, "Order reference missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadOrderDetails(ref)
    }

    private fun loadOrderDetails(ref: String) {
        lifecycleScope.launch {
            val result = repository.getOrderDetails(ref)
            result.fold(
                onSuccess = { order ->
                    bindOrderData(order)
                },
                onFailure = { error ->
                    Toast.makeText(this@OrderDetailsActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun bindOrderData(order: Order) {
        binding.tvOrderRef.text = order.orderReference
        binding.tvOrderStatus.text = order.orderStatus
        binding.tvCustomerInfo.text = "Customer: ${order.customerName} (${order.phoneNumber})"
        binding.tvDeliveryAddress.text = "Delivery Address: ${order.deliveryAddress}"

        val formatter = NumberFormat.getNumberInstance(Locale.US)
        binding.tvTotalAmount.text = "KES ${formatter.format(order.totalAmount)}"

        if (!order.receiptNumber.isNullOrEmpty()) {
            binding.tvReceiptNumber.text = "M-Pesa Receipt: ${order.receiptNumber}"
            binding.tvReceiptNumber.visibility = View.VISIBLE
        } else {
            binding.tvReceiptNumber.visibility = View.GONE
        }

        when (order.orderStatus) {
            "Paid", "Completed" -> {
                binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_status_success)
                binding.tvOrderStatus.setTextColor(ContextCompat.getColor(this, R.color.status_success_text))
            }
            "Cancelled" -> {
                binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_status_failed)
                binding.tvOrderStatus.setTextColor(ContextCompat.getColor(this, R.color.status_failed_text))
            }
            else -> {
                binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_status_pending)
                binding.tvOrderStatus.setTextColor(ContextCompat.getColor(this, R.color.status_pending_text))
            }
        }

        // Dynamically add order items to layout
        binding.layoutOrderItems.removeAllViews()
        order.items?.forEach { item ->
            val tv = TextView(this).apply {
                text = "• ${item.productName} (x${item.quantity}) - KES ${formatter.format(item.subtotal)}"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@OrderDetailsActivity, R.color.text_primary))
                setPadding(0, 6, 0, 6)
            }
            binding.layoutOrderItems.addView(tv)
        }
    }
}
