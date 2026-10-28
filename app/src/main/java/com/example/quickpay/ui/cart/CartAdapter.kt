package com.example.quickpay.ui.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.quickpay.R
import com.example.quickpay.data.model.CartItem
import com.example.quickpay.databinding.ItemCartBinding
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private var items: List<CartItem> = emptyList(),
    private val onQuantityChange: (CartItem, Int) -> Unit,
    private val onRemoveItem: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    fun updateList(newList: List<CartItem>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            binding.tvName.text = item.product.name
            binding.tvUnitPrice.text = "Unit: KES ${formatter.format(item.product.price)}"
            binding.tvSubtotal.text = "Subtotal: KES ${formatter.format(item.subtotal)}"
            binding.tvQuantity.text = item.quantity.toString()

            if (!item.product.imageUrl.isNullOrBlank()) {
                binding.ivProduct.load(item.product.imageUrl) {
                    placeholder(R.drawable.ic_launcher_background)
                    error(R.drawable.ic_launcher_background)
                }
            } else {
                binding.ivProduct.setImageResource(R.drawable.ic_launcher_background)
            }

            binding.btnPlus.setOnClickListener {
                onQuantityChange(item, item.quantity + 1)
            }

            binding.btnMinus.setOnClickListener {
                onQuantityChange(item, item.quantity - 1)
            }

            binding.btnRemove.setOnClickListener {
                onRemoveItem(item)
            }
        }
    }
}
