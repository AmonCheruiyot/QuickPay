package com.example.quickpay.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quickpay.databinding.ActivityTransactionHistoryBinding
import com.example.quickpay.ui.status.PaymentStatusActivity
import com.example.quickpay.utils.Constants

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionHistoryBinding
    private val viewModel: TransactionHistoryViewModel by viewModels()

    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()

        viewModel.loadHistory()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter { transaction ->
            val intent = Intent(this, PaymentStatusActivity::class.java).apply {
                putExtra(Constants.EXTRA_TRANSACTION_REFERENCE, transaction.reference)
            }
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is HistoryUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                    binding.recyclerView.visibility = View.GONE
                }
                is HistoryUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    adapter.updateList(state.transactions)
                }
                is HistoryUiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                }
                is HistoryUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = state.message
                    binding.recyclerView.visibility = View.GONE
                }
            }
        }
    }
}
