package com.example.holidaylist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ItemManagementActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var items: ArrayList<Item>
    private lateinit var adapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_management)

        recyclerView = findViewById(R.id.recyclerViewItems)
        recyclerView.layoutManager = LinearLayoutManager(this)
        items = arrayListOf()

        // Initialize the adapter with the delete function
        adapter = ItemAdapter(items, { item ->
            editItem(item)
        }, { item ->
            deleteItem(item)
        })

        recyclerView.adapter = adapter

        val buttonAddItem = findViewById<Button>(R.id.buttonAddItem)
        buttonAddItem.setOnClickListener {
            showAddItemDialog()
        }

        fetchItems()
    }





    private fun deleteItem(item: Item) {
        val db = FirebaseFirestore.getInstance()
        if (item.id.isNotEmpty()) {
            db.collection("items").document(item.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
                    items.remove(item)
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error deleting item", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun editItem(item: Item) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val editTextItemName = dialogView.findViewById<EditText>(R.id.editTextItemName)
        val editTextItemDescription = dialogView.findViewById<EditText>(R.id.editTextItemDescription)
        val editTextItemPrice = dialogView.findViewById<EditText>(R.id.editTextItemPrice)
        val editTextStoreLocation = dialogView.findViewById<EditText>(R.id.editTextStoreLocation)

        // Pre-fill the dialog with the item's current data
        editTextItemName.setText(item.name)
        editTextItemDescription.setText(item.description)
        editTextItemPrice.setText(item.price.toString())
        editTextStoreLocation.setText(item.storeLocation)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Edit Item")
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = dialogBuilder.create()

        alertDialog.setOnShowListener {
            val updateButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            updateButton.setOnClickListener {
                val updatedName = editTextItemName.text.toString().trim()
                val updatedDescription = editTextItemDescription.text.toString().trim()
                val updatedPrice = editTextItemPrice.text.toString().trim().toDoubleOrNull() ?: item.price
                val updatedLocation = editTextStoreLocation.text.toString().trim()

                if (updatedName.isNotEmpty() && updatedPrice > 0) {
                    updateItemInFirestore(item.id, updatedName, updatedDescription, updatedPrice, updatedLocation)
                    alertDialog.dismiss()
                } else {
                    Toast.makeText(this, "Name and price cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }

        alertDialog.show()
    }

    private fun updateItemInFirestore(id: String, name: String, description: String, price: Double, storeLocation: String) {
        val db = FirebaseFirestore.getInstance()
        val itemUpdate = mapOf(
            "name" to name,
            "description" to description,
            "price" to price,
            "storeLocation" to storeLocation
        )

        db.collection("items").document(id)
            .update(itemUpdate)
            .addOnSuccessListener {
                Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
                // Update the specific item in the local list and refresh the RecyclerView
                val index = items.indexOfFirst { it.id == id }
                if (index != -1) {
                    items[index] = Item(id, name, description, price, storeLocation)
                    adapter.notifyItemChanged(index)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating item", Toast.LENGTH_SHORT).show()
            }
    }



    private fun fetchItems() {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            db.collection("items")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val item = document.toObject(Item::class.java).apply {
                            id = document.id  // Assign the Firestore document ID here
                        }
                        items.add(item)
                    }
                    adapter.notifyDataSetChanged()
                }
            // ... handle failure ...
        }
        // ... handle no user logged in ...
    }


    private fun showAddItemDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val editTextItemName = dialogView.findViewById<EditText>(R.id.editTextItemName)
        val editTextItemDescription = dialogView.findViewById<EditText>(R.id.editTextItemDescription)
        val editTextItemPrice = dialogView.findViewById<EditText>(R.id.editTextItemPrice)
        val editTextStoreLocation = dialogView.findViewById<EditText>(R.id.editTextStoreLocation)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add New Item")
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", { dialog, _ -> dialog.dismiss() })

        val alertDialog = dialogBuilder.create()

        alertDialog.setOnShowListener {
            val addButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            addButton.setOnClickListener {
                val name = editTextItemName.text.toString().trim()
                val description = editTextItemDescription.text.toString().trim()
                val price = editTextItemPrice.text.toString().trim().toDoubleOrNull() ?: 0.0
                val storeLocation = editTextStoreLocation.text.toString().trim()

                if (name.isNotEmpty() && price > 0) {
                    addItemToFirestore(name, description, price, storeLocation)
                    alertDialog.dismiss()
                } else {
                    Toast.makeText(this, "Name and price cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }

        alertDialog.show()
    }

    private fun addItemToFirestore(name: String, description: String, price: Double, storeLocation: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val newItem = Item(name = name, description = description, price = price, storeLocation = storeLocation, userId = userId)

        val db = FirebaseFirestore.getInstance()
        db.collection("items").add(newItem)
            .addOnSuccessListener { documentReference ->
                newItem.id = documentReference.id
                items.add(newItem)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error adding item: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ItemManagement", "Error adding document", e)
            }
    }
}

data class Item(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var price: Double = 0.0,
    var storeLocation: String = "",
    var userId: String = ""  // User ID of the item's owner
)

class ItemAdapter(
    private val items: MutableList<Item>,
    private val onEditClick: (Item) -> Unit,
    private val onDeleteClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.textViewItemName)
        val editButton: Button = view.findViewById(R.id.buttonEdit)
        val deleteButton: Button = view.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.itemName.text = item.name
        holder.editButton.setOnClickListener { onEditClick(item) }
        holder.deleteButton.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = items.size
}


