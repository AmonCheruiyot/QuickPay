package com.example.quickpay.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.quickpay.data.repository.EcommerceRepository
import com.example.quickpay.databinding.ActivityLoginBinding
import com.example.quickpay.ui.admin.AdminDashboardActivity
import com.example.quickpay.ui.catalog.ProductCatalogActivity
import com.example.quickpay.utils.UserSessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val repository = EcommerceRepository()
    private lateinit var sessionManager: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = UserSessionManager(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnLogin.isEnabled = false

            lifecycleScope.launch {
                val result = repository.login(email, password)
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                result.fold(
                    onSuccess = { user ->
                        sessionManager.saveUserSession(user)
                        Toast.makeText(this@LoginActivity, "Welcome ${user.fullName}!", Toast.LENGTH_SHORT).show()

                        if (user.role == "admin") {
                            startActivity(Intent(this@LoginActivity, AdminDashboardActivity::class.java))
                        } else {
                            startActivity(Intent(this@LoginActivity, ProductCatalogActivity::class.java))
                        }
                        finish()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@LoginActivity, error.message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnGuest.setOnClickListener {
            sessionManager.logout()
            startActivity(Intent(this, ProductCatalogActivity::class.java))
            finish()
        }
    }
}
