package com.example.quickpay.ui.cart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quickpay.databinding.ActivityCartBinding
import com.example.quickpay.ui.checkout.CheckoutActivity
import com.example.quickpay.utils.CartManager
import java.text.NumberFormat
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        updateCartState()
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(
            onQuantityChange = { item, newQty ->
                if (CartManager.updateQuantity(item.product.id, newQty)) {
                    updateCartState()
                } else {
                    Toast.makeText(this, "Cannot exceed available stock (${item.product.stockQuantity})", Toast.LENGTH_SHORT).show()
                }
            },
            onRemoveItem = { item ->
                CartManager.removeItem(item.product.id)
                updateCartState()
            }
        )

        binding.rvCart.layoutManager = LinearLayoutManager(this)
        binding.rvCart.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnCheckout.setOnClickListener {
            if (CartManager.getItems().isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, CheckoutActivity::class.java))
        }
    }

    private fun updateCartState() {
        val items = CartManager.getItems()
        if (items.isEmpty()) {
            binding.tvEmptyCart.visibility = View.VISIBLE
            binding.rvCart.visibility = View.GONE
            binding.cvFooter.visibility = View.GONE
        } else {
            binding.tvEmptyCart.visibility = View.GONE
            binding.rvCart.visibility = View.VISIBLE
            binding.cvFooter.visibility = View.VISIBLE

            adapter.updateList(items)
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            binding.tvTotalAmount.text = "KES ${formatter.format(CartManager.getTotalAmount())}"
        }
    }
}
