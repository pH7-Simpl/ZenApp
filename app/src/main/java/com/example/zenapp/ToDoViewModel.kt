package com.example.zenapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ToDoViewModel : ViewModel() {
    private val repository = ToDoRepository()

    private val _toDoList = MutableStateFlow<List<ToDoItem>>(emptyList())
    val toDoList: StateFlow<List<ToDoItem>> = _toDoList

    fun loadToDoItems() {
        viewModelScope.launch {
            val items = repository.getToDoItems()    // Operasi asynchronous
            _toDoList.value = items                  // Mengubah state UI setelah data didapat
        }
    }

    fun addToDoItem(title: String, description: String) {
        val newItem = ToDoItem(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            isDone = false
        )
        viewModelScope.launch {
            repository.addToDoItem(newItem) // Operasi asynchronous menyimpan ke Firestore
            loadToDoItems() // Memuat ulang data setelah menambahkan item baru
            }
        }

    fun updateToDoItemStatus(itemId: String) {
        viewModelScope.launch {
            // Update the isDone property in the repository
            repository.updateToDoItemStatus(itemId)

            // Update the local toDoList to reflect the change
            val updatedList = _toDoList.value.map { item ->
                if (item.id == itemId) {
                    item.copy(isDone = !item.isDone)
                } else {
                    item
                }
            }
            _toDoList.value = updatedList
        }
    }

    fun deleteToDoItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteToDoItem(itemId) // Call delete function in repository
            val updatedList = _toDoList.value.filter { item -> item.id != itemId }
            _toDoList.value = updatedList // Update local state after deletion
        }
    }

    fun editToDoItem(itemId: String, title: String, description: String) {
        viewModelScope.launch {
            val itemToUpdate = ToDoItem(itemId, title, description) // Create a new item with updated details
            repository.editToDoItem(itemToUpdate)
            loadToDoItems() // Reload data after edit to reflect changes
        }
    }
}