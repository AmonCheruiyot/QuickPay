package com.example.quickpay.ui.catalog

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.quickpay.R
import com.example.quickpay.data.model.Product
import com.example.quickpay.databinding.ActivityProductDetailBinding
import com.example.quickpay.utils.CartManager
import java.text.NumberFormat
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private var product: Product? = null
    private var quantity = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getIntExtra("EXTRA_PRODUCT_ID", -1)
        val name = intent.getStringExtra("EXTRA_PRODUCT_NAME") ?: "Product"
        val desc = intent.getStringExtra("EXTRA_PRODUCT_DESC") ?: ""
        val price = intent.getIntExtra("EXTRA_PRODUCT_PRICE", 0)
        val stock = intent.getIntExtra("EXTRA_PRODUCT_STOCK", 0)
        val category = intent.getStringExtra("EXTRA_PRODUCT_CATEGORY") ?: "General"
        val imageUrl = intent.getStringExtra("EXTRA_PRODUCT_IMAGE")

        product = Product(id, name, desc, price, stock, category, imageUrl, true, stock > 0)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val prod = product ?: return
        binding.tvName.text = prod.name
        binding.tvCategory.text = prod.category
        binding.tvDescription.text = prod.description

        val formatter = NumberFormat.getNumberInstance(Locale.US)
        binding.tvPrice.text = "KES ${formatter.format(prod.price)}"

        if (!prod.imageUrl.isNullOrBlank()) {
            binding.ivProduct.load(prod.imageUrl) {
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }
        }

        binding.tvStock.text = if (prod.stockQuantity > 0) "In Stock: ${prod.stockQuantity}" else "Out of Stock"
        binding.tvQuantity.text = quantity.toString()
        binding.btnAddToCart.isEnabled = prod.stockQuantity > 0
    }

    private fun setupListeners() {
        binding.btnPlus.setOnClickListener {
            val stock = product?.stockQuantity ?: 0
            if (quantity < stock) {
                quantity++
                binding.tvQuantity.text = quantity.toString()
            } else {
                Toast.makeText(this, "Cannot exceed available stock ($stock)", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.tvQuantity.text = quantity.toString()
            }
        }

        binding.btnAddToCart.setOnClickListener {
            val prod = product ?: return@setOnClickListener
            if (CartManager.addItem(prod, quantity)) {
                Toast.makeText(this, "${prod.name} (x$quantity) added to cart!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Cannot add more than available stock", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
