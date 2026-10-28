package com.example.quickpay.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.quickpay.R
import com.example.quickpay.data.model.Order
import com.example.quickpay.databinding.ItemOrderBinding
import java.text.NumberFormat
import java.util.Locale

class OrderAdapter(
    private var orders: List<Order> = emptyList(),
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    fun updateList(newList: List<Order>) {
        orders = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    inner class ViewHolder(private val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Order) {
            val context = binding.root.context
            binding.tvOrderReference.text = item.orderReference
            binding.tvOrderDate.text = "Date: ${item.createdAt}"
            binding.tvItemSummary.text = "${item.itemCount ?: item.items?.size ?: 1} item(s)"

            val formatter = NumberFormat.getNumberInstance(Locale.US)
            binding.tvTotalAmount.text = "KES ${formatter.format(item.totalAmount)}"

            binding.tvOrderStatusBadge.text = item.orderStatus

            when (item.orderStatus) {
                "Paid", "Completed" -> {
                    binding.tvOrderStatusBadge.setBackgroundResource(R.drawable.bg_status_success)
                    binding.tvOrderStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_success_text))
                    if (!item.receiptNumber.isNullOrBlank()) {
                        binding.tvReceipt.text = "Receipt: ${item.receiptNumber}"
                        binding.tvReceipt.visibility = View.VISIBLE
                    } else {
                        binding.tvReceipt.visibility = View.GONE
                    }
                }
                "Cancelled" -> {
                    binding.tvOrderStatusBadge.setBackgroundResource(R.drawable.bg_status_failed)
                    binding.tvOrderStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_failed_text))
                    binding.tvReceipt.visibility = View.GONE
                }
                else -> { // Pending Payment, Processing
                    binding.tvOrderStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                    binding.tvOrderStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_pending_text))
                    binding.tvReceipt.visibility = View.GONE
                }
            }

            binding.root.setOnClickListener {
                onOrderClick(item)
            }
        }
    }
}
