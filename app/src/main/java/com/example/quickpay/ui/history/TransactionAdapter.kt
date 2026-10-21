package com.example.quickpay.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.quickpay.R
import com.example.quickpay.data.model.Transaction
import com.example.quickpay.databinding.ItemTransactionBinding

class TransactionAdapter(
    private var transactions: List<Transaction> = emptyList(),
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    fun updateList(newList: List<Transaction>) {
        transactions = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    inner class ViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Transaction) {
            val context = binding.root.context
            binding.tvAmount.text = "KSh ${item.amount}"
            binding.tvReference.text = "Ref: ${item.reference}"
            binding.tvPhone.text = item.maskedPhone ?: item.phone
            binding.tvDate.text = item.createdAt

            binding.tvStatusBadge.text = item.status.uppercase()

            when (item.status.uppercase()) {
                "SUCCESS" -> {
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_success)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_success_text))
                    if (!item.receiptNumber.isNull_or_empty()) {
                        binding.tvReceipt.text = "Receipt: ${item.receiptNumber}"
                        binding.tvReceipt.visibility = View.VISIBLE
                    } else {
                        binding.tvReceipt.visibility = View.GONE
                    }
                }
                "FAILED" -> {
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_failed)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_failed_text))
                    binding.tvReceipt.visibility = View.GONE
                }
                "CANCELLED" -> {
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_cancelled)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_cancelled_text))
                    binding.tvReceipt.visibility = View.GONE
                }
                else -> { // PENDING
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_pending_text))
                    binding.tvReceipt.visibility = View.GONE
                }
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
}
