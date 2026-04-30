package com.crewsync.crewsync

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.crewsync.crewsync.data.CategoryStore
import com.crewsync.crewsync.data.EventStore
import com.crewsync.crewsync.data.UserPrefs
import com.crewsync.crewsync.db.CrewSyncDatabase
import kotlinx.coroutines.launch

class CategoryManagerActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<String>
    private var categories: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_manager)

        val currentUser = UserPrefs.getCurrentUserId(this)
        if (currentUser != "manager") {
            Toast.makeText(this, "Manager only", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val etNewCategory = findViewById<EditText>(R.id.etNewCategory)
        val btnAddCategory = findViewById<Button>(R.id.btnAddCategory)
        val listCategories = findViewById<ListView>(R.id.listCategories)

        fun refresh() {
            categories = CategoryStore.getCategories(this).toMutableList()
            adapter.clear()
            adapter.addAll(categories)
            adapter.notifyDataSetChanged()
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listCategories.adapter = adapter
        refresh()

        btnAddCategory.setOnClickListener {
            val category = etNewCategory.text.toString().trim()

            if (category.isBlank()) {
                Toast.makeText(this, "Category name required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val added = CategoryStore.addCategory(this, category)

            if (!added) {
                Toast.makeText(this, "Category already exists or is invalid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            etNewCategory.text.clear()
            Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
            refresh()
        }

        listCategories.setOnItemClickListener { _, _, pos, _ ->
            val selected = categories[pos]
            showCategoryOptions(selected, ::refresh)
        }
    }

    private fun showCategoryOptions(category: String, refresh: () -> Unit) {
        val options =
            if (category.equals("General", ignoreCase = true)) {
                arrayOf("Edit Category")
            } else {
                arrayOf("Edit Category", "Delete Category")
            }

        AlertDialog.Builder(this)
            .setTitle(category)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Edit Category" -> showRenameDialog(category, refresh)
                    "Delete Category" -> confirmDeleteCategory(category, refresh)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog(oldCategory: String, refresh: () -> Unit) {
        val input = EditText(this)
        input.hint = "Category name"
        input.setText(oldCategory)

        AlertDialog.Builder(this)
            .setTitle("Edit Category")
            .setView(input)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newCategory = input.text.toString().trim()

                        if (newCategory.isBlank()) {
                            Toast.makeText(this@CategoryManagerActivity, "Category name required", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val renamed = CategoryStore.renameCategory(this@CategoryManagerActivity, oldCategory, newCategory)

                        if (!renamed) {
                            Toast.makeText(this@CategoryManagerActivity, "Could not rename category", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        lifecycleScope.launch {
                            val dao = CrewSyncDatabase.getInstance(this@CategoryManagerActivity).taskDao()
                            dao.updateTaskCategory(oldCategory, newCategory)
                            EventStore.updateCategoryForCustomEvents(this@CategoryManagerActivity, oldCategory, newCategory)

                            Toast.makeText(this@CategoryManagerActivity, "Category updated", Toast.LENGTH_SHORT).show()
                            refresh()
                            dismiss()
                        }
                    }
                }
            }
            .show()
    }

    private fun confirmDeleteCategory(category: String, refresh: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Delete $category?")
            .setMessage("Tasks and saved events using this category will be moved to General.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory(category, refresh)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(category: String, refresh: () -> Unit) {
        val deleted = CategoryStore.deleteCategory(this, category)

        if (!deleted) {
            Toast.makeText(this, "Could not delete category", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val dao = CrewSyncDatabase.getInstance(this@CategoryManagerActivity).taskDao()
            dao.updateTaskCategory(category, "General")
            EventStore.updateCategoryForCustomEvents(this@CategoryManagerActivity, category, "General")

            Toast.makeText(this@CategoryManagerActivity, "Category deleted", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }
}
