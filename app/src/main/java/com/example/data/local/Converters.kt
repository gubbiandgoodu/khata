package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.PaymentMode

class Converters {
    @TypeConverter
    fun fromPaymentMode(mode: PaymentMode?): String {
        return mode?.name ?: PaymentMode.CASH.name
    }

    @TypeConverter
    fun toPaymentMode(value: String?): PaymentMode {
        return if (value == null) {
            PaymentMode.CASH
        } else {
            try {
                PaymentMode.valueOf(value)
            } catch (_: IllegalArgumentException) {
                PaymentMode.CASH
            }
        }
    }
}
