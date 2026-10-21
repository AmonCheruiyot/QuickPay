package com.example.quickpay.ui.status

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.quickpay.R
import com.example.quickpay.data.model.Transaction
import com.example.quickpay.databinding.ActivityPaymentStatusBinding
import com.example.quickpay.utils.Constants

class PaymentStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentStatusBinding
    private val viewModel: PaymentStatusViewModel by viewModels()

    private var reference: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reference = intent.getStringExtra(Constants.EXTRA_TRANSACTION_REFERENCE)

        if (reference.isNullOrEmpty()) {
            Toast.makeText(this, "Transaction reference missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
        observeViewModel()

        viewModel.fetchPaymentStatus(reference!!)
    }

    private fun setupListeners() {
        binding.btnRefresh.setOnClickListener {
            reference?.let { ref ->
                viewModel.fetchPaymentStatus(ref)
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is StatusUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnRefresh.isEnabled = false
                }
                is StatusUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRefresh.isEnabled = true
                    bindTransaction(state.transaction)
                }
                is StatusUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRefresh.isEnabled = true
                    binding.tvExplanation.text = state.message
                    binding.tvExplanation.setTextColor(ContextCompat.getColor(this, R.color.status_failed_text))
                }
            }
        }
    }

    private fun bindTransaction(transaction: Transaction) {
        binding.tvReference.text = transaction.reference
        binding.tvAmount.text = "KSh ${transaction.amount}"
        binding.tvPhone.text = transaction.maskedPhone ?: transaction.phone
        binding.tvDate.text = transaction.createdAt

        binding.tvStatusBadge.text = transaction.status.uppercase()

        when (transaction.status.uppercase()) {
            "SUCCESS" -> {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_success)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_success_text))
                binding.tvExplanation.text = "Payment confirmed successfully via Safaricom M-Pesa callback!"
                binding.tvExplanation.setTextColor(ContextCompat.getColor(this, R.color.status_success_text))

                if (!transaction.receiptNumber.isNullOrEmpty()) {
                    binding.tvReceipt.text = transaction.receiptNumber
                    binding.layoutReceipt.visibility = View.VISIBLE
                } else {
                    binding.layoutReceipt.visibility = View.GONE
                }
            }
            "FAILED" -> {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_failed)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_failed_text))
                val reason = transaction.resultDescription ?: "Payment failed or rejected by customer"
                binding.tvExplanation.text = "Transaction Failed: $reason"
                binding.tvExplanation.setTextColor(ContextCompat.getColor(this, R.color.status_failed_text))
                binding.layoutReceipt.visibility = View.GONE
            }
            "CANCELLED" -> {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_cancelled)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_cancelled_text))
                binding.tvExplanation.text = "Transaction was cancelled by the customer on their phone"
                binding.tvExplanation.setTextColor(ContextCompat.getColor(this, R.color.status_cancelled_text))
                binding.layoutReceipt.visibility = View.GONE
            }
            else -> { // PENDING
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_pending_text))
                binding.tvExplanation.text = "Awaiting customer M-Pesa PIN prompt on phone..."
                binding.tvExplanation.setTextColor(ContextCompat.getColor(this, R.color.status_pending_text))
                binding.layoutReceipt.visibility = View.GONE
            }
        }
    }
}
