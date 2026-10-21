package com.example.quickpay

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.quickpay.databinding.ActivityMainBinding
import com.example.quickpay.ui.history.TransactionHistoryActivity
import com.example.quickpay.ui.main.MainViewModel
import com.example.quickpay.ui.main.PaymentUiState
import com.example.quickpay.ui.status.PaymentStatusActivity
import com.example.quickpay.utils.Constants

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnPay.setOnClickListener {
            val phone = binding.etPhone.text?.toString() ?: ""
            val amount = binding.etAmount.text?.toString() ?: ""
            val serverUrl = binding.etServerUrl.text?.toString() ?: Constants.DEFAULT_BASE_URL

            viewModel.setBaseUrl(serverUrl)
            viewModel.initiatePayment(phone, amount)
        }

        binding.btnHistory.setOnClickListener {
            val intent = Intent(this, TransactionHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is PaymentUiState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPay.isEnabled = true
                    binding.tvMessage.visibility = View.GONE
                }
                is PaymentUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnPay.isEnabled = false
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.tvMessage.text = "Initiating Lipa Na M-Pesa STK Push request..."
                    binding.tvMessage.setTextColor(getColor(R.color.text_secondary))
                }
                is PaymentUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPay.isEnabled = true
                    binding.tvMessage.visibility = View.GONE

                    Toast.makeText(
                        this,
                        state.response.message ?: "STK Push initiated successfully!",
                        Toast.LENGTH_LONG
                    ).show()

                    // Navigate to Payment Status screen
                    val reference = state.response.reference
                    if (!reference.isNullOrEmpty()) {
                        val intent = Intent(this, PaymentStatusActivity::class.java).apply {
                            putExtra(Constants.EXTRA_TRANSACTION_REFERENCE, reference)
                        }
                        startActivity(intent)
                    }
                    viewModel.resetState()
                }
                is PaymentUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPay.isEnabled = true
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.tvMessage.text = state.message
                    binding.tvMessage.setTextColor(getColor(R.color.status_failed_text))
                }
            }
        }
    }
}
