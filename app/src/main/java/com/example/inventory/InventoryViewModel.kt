package com.example.inventory

import android.icu.text.AlphabeticIndex
import androidx.lifecycle.*
import com.example.inventory.data.Item
import com.example.inventory.data.ItemDao
import com.example.inventory.data.Record
import kotlinx.coroutines.launch

class InventoryViewModel(private val itemDao: ItemDao) : ViewModel() {
    val allItems: LiveData<List<Item>> = itemDao.getItems().asLiveData()
    private fun insertItem(item: Item) {
        viewModelScope.launch {
            itemDao.insert(item)
        }
    }

    private fun getNewItemEntry(
        itemName: String,
        itemPrice: String,
        itemCount: String,
        itemProviderName: String,
        itemProviderEmail: String,
        itemProviderPhoneNumber: String
    ): Item {
        return Item(
            itemName = itemName,
            itemPrice = itemPrice.toDouble(),
            quantityInStock = itemCount.toInt(),
            providerName = itemProviderName,
            providerEmail = itemProviderEmail,
            providerPhoneNumber = itemProviderPhoneNumber
        )
    }

    private fun updateItem(item: Item) {
        viewModelScope.launch {
            itemDao.update(item)
        }
    }

    private fun getUpdatedItemEntry(
        itemId: Int,
        itemName: String,
        itemPrice: String,
        itemCount: String,
        itemProviderName: String,
        itemProviderEmail: String,
        itemProviderPhoneNumber: String
    ): Item {
        return Item(
            id = itemId,
            itemName = itemName,
            itemPrice = itemPrice.toDouble(),
            quantityInStock = itemCount.toInt(),
            providerName = itemProviderName,
            providerEmail = itemProviderEmail,
            providerPhoneNumber = itemProviderPhoneNumber
        )
    }

    fun updateItem(
        itemId: Int,
        itemName: String,
        itemPrice: String,
        itemCount: String,
        itemProviderName: String,
        itemProviderEmail: String,
        itemProviderPhoneNumber: String
    ) {
        val updatedItem = getUpdatedItemEntry(itemId, itemName, itemPrice, itemCount, itemProviderName, itemProviderEmail, itemProviderPhoneNumber)
        updateItem(updatedItem)
    }

    fun sellItem(item: Item) {
        if (item.quantityInStock > 0) {
            val newItem = item.copy(quantityInStock = item.quantityInStock - 1)
            updateItem(newItem)
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            itemDao.delete(item)
        }
    }

    fun addNewItem(item: Item) {
        item.itemTypeRecord = Record.FILE
        insertItem(item)
    }

    fun addNewItem(
        itemName: String,
        itemPrice: String,
        itemCount: String,
        itemProviderName: String,
        itemProviderEmail: String,
        itemProviderPhoneNumber: String
    ) {
        val newItem = getNewItemEntry(itemName, itemPrice, itemCount, itemProviderName, itemProviderEmail, itemProviderPhoneNumber)
        insertItem(newItem)
    }

    fun isEntryValid(
        itemName: String,
        itemPrice: String,
        itemCount: String,
        itemProviderName: String,
        itemProviderEmail: String,
        itemProviderPhoneNumber: String
    ): Boolean {
        if (itemName.isBlank() ||
            itemPrice.isBlank() ||
            itemCount.isBlank() ||
            itemProviderName.isBlank() ||
            itemProviderEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(itemProviderEmail).matches() ||
            itemProviderPhoneNumber.isBlank() || !android.util.Patterns.PHONE.matcher(itemProviderPhoneNumber).matches()) {
            return false
        }
        return true
    }

    fun retrieveItem(id: Int): LiveData<Item> {
        return itemDao.getItem(id).asLiveData()
    }

    fun isStockAvailable(item: Item): Boolean {
        return (item.quantityInStock > 0)
    }
}

class InventoryViewModelFactory(private val itemDao: ItemDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(itemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}