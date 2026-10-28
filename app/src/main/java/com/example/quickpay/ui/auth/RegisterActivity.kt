package com.example.quickpay.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.quickpay.data.repository.EcommerceRepository
import com.example.quickpay.databinding.ActivityRegisterBinding
import com.example.quickpay.ui.catalog.ProductCatalogActivity
import com.example.quickpay.utils.UserSessionManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val repository = EcommerceRepository()
    private lateinit var sessionManager: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = UserSessionManager(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegisterSubmit.setOnClickListener {
            val name = binding.etFullName.text?.toString() ?: ""
            val email = binding.etEmail.text?.toString() ?: ""
            val phone = binding.etPhone.text?.toString() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            val address = binding.etAddress.text?.toString() ?: ""

            if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnRegisterSubmit.isEnabled = false

            lifecycleScope.launch {
                val result = repository.register(name, email, phone, password, address)
                binding.progressBar.visibility = View.GONE
                binding.btnRegisterSubmit.isEnabled = true

                result.fold(
                    onSuccess = { user ->
                        sessionManager.saveUserSession(user)
                        Toast.makeText(this@RegisterActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegisterActivity, ProductCatalogActivity::class.java))
                        finish()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@RegisterActivity, error.message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
