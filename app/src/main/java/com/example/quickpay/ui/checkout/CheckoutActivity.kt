package com.example.quickpay.ui.checkout

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.quickpay.data.model.User
import com.example.quickpay.databinding.ActivityCheckoutBinding
import com.example.quickpay.ui.status.PaymentStatusActivity
import com.example.quickpay.utils.CartManager
import com.example.quickpay.utils.Constants
import com.example.quickpay.utils.UserSessionManager
import java.text.NumberFormat
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private val viewModel: CheckoutViewModel by viewModels()

    private lateinit var sessionManager: UserSessionManager
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = UserSessionManager(this)
        currentUser = sessionManager.getUser()

        setupUI()
        setupListeners()
        observeViewModel()
    }

    private fun setupUI() {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        val total = CartManager.getTotalAmount()
        binding.tvTotalPayable.text = "KES ${formatter.format(total)}"

        val itemsSummary = CartManager.getItems().joinToString("\n") {
            "• ${it.product.name} (x${it.quantity}) - KES ${formatter.format(it.subtotal)}"
        }
        binding.tvOrderItemsSummary.text = itemsSummary

        currentUser?.let { user ->
            binding.etName.setText(user.fullName)
            binding.etPhone.setText(user.phone)
            if (!user.address.isNullOrBlank()) {
                binding.etAddress.setText(user.address)
            }
        }
    }

    private fun setupListeners() {
        binding.btnPayMpesa.setOnClickListener {
            val name = binding.etName.text?.toString() ?: ""
            val phone = binding.etPhone.text?.toString() ?: ""
            val address = binding.etAddress.text?.toString() ?: ""
            val serverUrl = binding.etServerUrl.text?.toString() ?: Constants.DEFAULT_BASE_URL

            viewModel.processCheckoutAndPay(
                userId = currentUser?.id,
                customerName = name,
                phoneInput = phone,
                deliveryAddress = address,
                baseUrl = serverUrl
            )
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CheckoutUiState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPayMpesa.isEnabled = true
                }
                is CheckoutUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnPayMpesa.isEnabled = false
                    Toast.makeText(this, "Creating order & requesting M-Pesa STK Push...", Toast.LENGTH_SHORT).show()
                }
                is CheckoutUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPayMpesa.isEnabled = true

                    Toast.makeText(
                        this,
                        state.paymentResponse.message ?: "STK Push sent to your phone! Please enter M-Pesa PIN.",
                        Toast.LENGTH_LONG
                    ).show()

                    val ref = state.paymentResponse.reference
                    if (!ref.isNullOrEmpty()) {
                        val intent = Intent(this, PaymentStatusActivity::class.java).apply {
                            putExtra(Constants.EXTRA_TRANSACTION_REFERENCE, ref)
                        }
                        startActivity(intent)
                        finish()
                    }
                }
                is CheckoutUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPayMpesa.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
