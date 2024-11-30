package com.example.zenapp

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ToDoRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun addToDoItem(item: ToDoItem) {
        db.collection("todos")
            .document(item.id)
            .set(item)
            .await()
    }

    suspend fun getToDoItems(): List<ToDoItem> {
        val result = db.collection("todos").get().await()
        return result.documents.mapNotNull { doc ->
            doc.toObject(ToDoItem::class.java)
            }
    }

    suspend fun updateToDoItemStatus(itemId: String) {
        val todoRef = db.collection("todos").document(itemId)
        val updateData = hashMapOf<String, Any>("isDone" to !isDone(itemId)) // Update the isDone property
        todoRef.update(updateData).await()
    }

    private suspend fun isDone(itemId: String): Boolean {
        val doc = db.collection("todos").document(itemId).get().await()
        return doc.getBoolean("isDone") ?: false  // Return false if isDone is missing
    }

    // Delete function
    suspend fun deleteToDoItem(itemId: String) {
        db.collection("todos").document(itemId).delete().await()
    }

    suspend fun editToDoItem(item: ToDoItem) {
        db.collection("todos").document(item.id).set(item).await() // Replace "itemId" with item.id
    }
}