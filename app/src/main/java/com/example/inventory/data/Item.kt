package com.example.inventory.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.NumberFormat

@Entity(tableName = "item")
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "name")
    val itemName: String,
    @ColumnInfo(name = "price")
    val itemPrice: Double,
    @ColumnInfo(name = "quantity")
    val quantityInStock: Int,
    @ColumnInfo(name = "provider_name")
    val providerName: String,
    @ColumnInfo(name = "provider_email")
    val providerEmail: String,
    @ColumnInfo(name = "provider_phone_number")
    val providerPhoneNumber: String
) {
    override fun toString(): String {
        return "Информация о товаре:\n" +
                "Название: ${itemName}\n" +
                "Цена: ${itemPrice}\n" +
                "Количество на складе: ${quantityInStock}\n" +
                "\nИнформация о поставщике:\n" +
                "Имя (название организации): ${providerName}\n" +
                "Email: ${providerEmail}\n" +
                "Номер телефона: $providerPhoneNumber"
    }
}

fun Item.getFormattedPrice(): String =
    NumberFormat.getCurrencyInstance().format(itemPrice)

