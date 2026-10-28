package com.example.quickpay.ui.catalog

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.quickpay.data.model.Product
import com.example.quickpay.databinding.ActivityProductCatalogBinding
import com.example.quickpay.ui.auth.LoginActivity
import com.example.quickpay.ui.cart.CartActivity
import com.example.quickpay.ui.orders.OrderHistoryActivity
import com.example.quickpay.utils.CartManager
import com.example.quickpay.utils.UserSessionManager
import kotlin.math.max

class ProductCatalogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductCatalogBinding
    private val viewModel: CatalogViewModel by viewModels()

    private lateinit var adapter: ProductAdapter
    private lateinit var sessionManager: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = UserSessionManager(this)

        setupUI()
        setupListeners()
        observeViewModel()

        viewModel.loadProducts()
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
        updateUserHeader()
    }

    private fun setupUI() {
        adapter = ProductAdapter(
            onProductClick = { product ->
                val intent = Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra("EXTRA_PRODUCT_ID", product.id)
                    putExtra("EXTRA_PRODUCT_NAME", product.name)
                    putExtra("EXTRA_PRODUCT_DESC", product.description)
                    putExtra("EXTRA_PRODUCT_PRICE", product.price)
                    putExtra("EXTRA_PRODUCT_STOCK", product.stockQuantity)
                    putExtra("EXTRA_PRODUCT_CATEGORY", product.category)
                    putExtra("EXTRA_PRODUCT_IMAGE", product.imageUrl)
                }
                startActivity(intent)
            },
            onAddToCartClick = { product ->
                if (CartManager.addItem(product, 1)) {
                    Toast.makeText(this, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                    updateCartBadge()
                } else {
                    Toast.makeText(this, "Requested quantity exceeds available stock", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Calculate responsive span count based on screen width
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val spanCount = max(2, (screenWidthDp / 170).toInt())

        binding.rvProducts.layoutManager = GridLayoutManager(this, spanCount)
        binding.rvProducts.adapter = adapter
    }

    private fun setupListeners() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnCatAll.setOnClickListener { viewModel.setCategoryFilter("All") }
        binding.btnCatPhones.setOnClickListener { viewModel.setCategoryFilter("Smartphones") }
        binding.btnCatAccessories.setOnClickListener { viewModel.setCategoryFilter("Accessories") }
        binding.btnCatLaptops.setOnClickListener { viewModel.setCategoryFilter("Laptops") }
        binding.btnCatFashion.setOnClickListener { viewModel.setCategoryFilter("Fashion") }

        binding.btnCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        binding.btnOrders.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CatalogUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvProducts.visibility = View.GONE
                }
                is CatalogUiState.Success -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvProducts.visibility = View.VISIBLE
                    adapter.updateList(state.products)
                    binding.progressBar.visibility = View.GONE
                }
                is CatalogUiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvProducts.visibility = View.GONE
                    Toast.makeText(this, "No products found", Toast.LENGTH_SHORT).show()
                }
                is CatalogUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvProducts.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateCartBadge() {
        val count = CartManager.getItemCount()
        if (count > 0) {
            binding.tvCartBadge.text = count.toString()
            binding.tvCartBadge.visibility = View.VISIBLE
        } else {
            binding.tvCartBadge.visibility = View.GONE
        }
    }

    private fun updateUserHeader() {
        val user = sessionManager.getUser()
        if (user != null) {
            binding.tvUserWelcome.text = "Hello, ${user.fullName}"
        } else {
            binding.tvUserWelcome.text = "Welcome, Guest"
        }
    }
}
