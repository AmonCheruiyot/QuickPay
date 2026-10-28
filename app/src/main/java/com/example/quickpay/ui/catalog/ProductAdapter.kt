package com.example.quickpay.ui.catalog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.quickpay.R
import com.example.quickpay.data.model.Product
import com.example.quickpay.databinding.ItemProductBinding
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private var products: List<Product> = emptyList(),
    private val onProductClick: (Product) -> Unit,
    private val onAddToCartClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    fun updateList(newList: List<Product>) {
        products = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    inner class ViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Product) {
            val context = binding.root.context
            binding.tvName.text = item.name
            binding.tvCategory.text = item.category

            val formatter = NumberFormat.getNumberInstance(Locale.US)
            binding.tvPrice.text = "KES ${formatter.format(item.price)}"

            if (item.imageUrl.isNullOrBlank()) {
                binding.ivProduct.setImageResource(R.drawable.ic_launcher_background)
            } else {
                binding.ivProduct.load(item.imageUrl) {
                    placeholder(R.drawable.ic_launcher_background)
                    error(R.drawable.ic_launcher_background)
                }
            }

            if (item.inStock && item.stockQuantity > 0) {
                binding.tvStock.text = "In Stock: ${item.stockQuantity}"
                binding.tvStock.setBackgroundResource(R.drawable.bg_status_success)
                binding.tvStock.setTextColor(ContextCompat.getColor(context, R.color.status_success_text))
                binding.btnAddToCart.isEnabled = true
                binding.btnAddToCart.text = "Add to Cart"
            } else {
                binding.tvStock.text = "Out of Stock"
                binding.tvStock.setBackgroundResource(R.drawable.bg_status_failed)
                binding.tvStock.setTextColor(ContextCompat.getColor(context, R.color.status_failed_text))
                binding.btnAddToCart.isEnabled = false
                binding.btnAddToCart.text = "Out of Stock"
            }

            binding.btnAddToCart.setOnClickListener {
                if (item.inStock) {
                    onAddToCartClick(item)
                }
            }

            binding.root.setOnClickListener {
                onProductClick(item)
            }
        }
    }
}
