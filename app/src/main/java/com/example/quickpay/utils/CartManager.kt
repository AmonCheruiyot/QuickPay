package com.example.quickpay.utils

import com.example.quickpay.data.model.CartItem
import com.example.quickpay.data.model.Product

object CartManager {

    private val cartItems = mutableListOf<CartItem>()

    fun addItem(product: Product, quantity: Int = 1): Boolean {
        val existing = cartItems.find { it.product.id == product.id }
        val currentQty = existing?.quantity ?: 0
        val newQty = currentQty + quantity

        if (newQty > product.stockQuantity) {
            return false // Cannot exceed available stock
        }

        if (existing != null) {
            existing.quantity = newQty
        } else {
            cartItems.add(CartItem(product, quantity))
        }
        return true
    }

    fun updateQuantity(productId: Int, newQuantity: Int): Boolean {
        val existing = cartItems.find { it.product.id == productId } ?: return false
        if (newQuantity <= 0) {
            cartItems.remove(existing)
            return true
        }

        if (newQuantity > existing.product.stockQuantity) {
            return false // Stock limit
        }

        existing.quantity = newQuantity
        return true
    }

    fun removeItem(productId: Int) {
        cartItems.removeAll { it.product.id == productId }
    }

    fun clearCart() {
        cartItems.clear()
    }

    fun getItems(): List<CartItem> = cartItems.toList()

    fun getItemCount(): Int = cartItems.sumOf { it.quantity }

    fun getTotalAmount(): Int = cartItems.sumOf { it.subtotal }
}
