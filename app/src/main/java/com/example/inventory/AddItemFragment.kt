/*
 * Copyright (C) 2021 The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.inventory

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.inventory.data.Item
import com.example.inventory.databinding.FragmentAddItemBinding
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/**
 * Fragment to add or update an item in the Inventory database.
 */
class AddItemFragment : Fragment() {

    private val viewModel: InventoryViewModel by activityViewModels {
        InventoryViewModelFactory(
            (activity?.application as InventoryApplication).database
                .itemDao()
        )
    }

    lateinit var item: Item
    private val PREFS_FILE = "Setting_Key"


    private val navigationArgs: ItemDetailFragmentArgs by navArgs()

    // Binding object instance corresponding to the fragment_add_item.xml layout
    // This property is non-null between the onCreateView() and onDestroyView() lifecycle callbacks,
    // when the view hierarchy is attached to the fragment
    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun isEntryValid(): Boolean {
        return viewModel.isEntryValid(
            binding.itemName.text.toString(),
            binding.itemPrice.text.toString(),
            binding.itemCount.text.toString(),
            binding.itemProviderName.text.toString(),
            binding.itemProviderEmail.text.toString(),
            binding.itemProviderPhoneNumber.text.toString()
        )
    }

    private fun addNewItem() {
        if (isEntryValid()) {
            viewModel.addNewItem(
                binding.itemName.text.toString(),
                binding.itemPrice.text.toString(),
                binding.itemCount.text.toString(),
                binding.itemProviderName.text.toString(),
                binding.itemProviderEmail.text.toString(),
                binding.itemProviderPhoneNumber.text.toString()
            )
            val action = AddItemFragmentDirections.actionAddItemFragmentToItemListFragment()
            findNavController().navigate(action)
        }
    }

    private fun bind(item: Item) {
        val price = "%.2f".format(item.itemPrice).replace(',', '.')
        binding.apply {
            itemName.setText(item.itemName, TextView.BufferType.SPANNABLE)
            itemPrice.setText(price, TextView.BufferType.SPANNABLE)
            itemCount.setText(item.quantityInStock.toString(), TextView.BufferType.SPANNABLE)
            itemProviderName.setText(item.providerName, TextView.BufferType.SPANNABLE)
            itemProviderEmail.setText(item.providerEmail, TextView.BufferType.SPANNABLE)
            itemProviderPhoneNumber.setText(item.providerPhoneNumber, TextView.BufferType.SPANNABLE)
            saveAction.setOnClickListener { updateItem() }
            loadFromFileBtn.setOnClickListener { openFile(Uri.parse("/")) }
        }
    }

    private fun openFile(pickerInitialUri: Uri) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
        requestUri.launch(intent)
    }

    @SuppressLint("Range")
    private fun dumpFileName(uri: Uri) : String{
        // The query, because it only applies to a single document, returns only
        // one row. There's no need to filter, sort, or select fields,
        // because we want all fields for one document.
        val cursor: Cursor? = requireActivity().contentResolver.query(
            uri, null, null, null, null, null)

        cursor?.use {
            // moveToFirst() returns false if the cursor has 0 rows. Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (it.moveToFirst()) {

                // Note it's called "Display Name". This is
                // provider-specific, and might not necessarily be the file name.
                val displayName: String =
                    it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))

                return displayName
            }
        }
        return ""
    }

    private var requestUri = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result != null && result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                intent.data?.let { fileUri ->
                    val mainKey = MasterKey.Builder(requireActivity().applicationContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    val cacheFileToWrite  = File(requireActivity().applicationContext.cacheDir, dumpFileName(fileUri))

                    requireActivity().applicationContext.contentResolver.openInputStream(fileUri)!!.copyTo(
                        requireActivity().applicationContext.contentResolver.openOutputStream(cacheFileToWrite.toUri())!!
                    )
                    val encryptedFile = EncryptedFile.Builder(
                        requireActivity().applicationContext,
                        cacheFileToWrite,
                        mainKey,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                    ).build()

                    try {
                        val inputStream = encryptedFile.openFileInput()
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        var nextByte: Int = inputStream.read()
                        while (nextByte != -1) {
                            byteArrayOutputStream.write(nextByte)
                            nextByte = inputStream.read()
                        }

                        val plaintext = byteArrayOutputStream.toByteArray()

                        val gson = Gson()
                        val item = gson.fromJson(plaintext.toString(Charsets.UTF_8), Item::class.java)


                        viewModel.addNewItem(item)
                        bind(item)

                    }
                    catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun updateItem() {
        if (isEntryValid()) {
            viewModel.updateItem(
                this.navigationArgs.itemId,
                this.binding.itemName.text.toString(),
                this.binding.itemPrice.text.toString(),
                this.binding.itemCount.text.toString(),
                this.binding.itemProviderName.text.toString(),
                this.binding.itemProviderEmail.text.toString(),
                this.binding.itemProviderPhoneNumber.text.toString()
            )
            val action = AddItemFragmentDirections.actionAddItemFragmentToItemListFragment()
            findNavController().navigate(action)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sharedPreferences = EncryptedSharedPreferences.create(
            requireContext(),
            PREFS_FILE,
            MasterKey.Builder(requireContext()).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        super.onViewCreated(view, savedInstanceState)
        val id = navigationArgs.itemId
        if (id > 0) {
            viewModel.retrieveItem(id).observe(this.viewLifecycleOwner) { selectedItem ->
                item = selectedItem
                bind(item)
            }
        } else {
            if (sharedPreferences.getBoolean("DefaultValues", false)) {
                binding.apply {
                    itemProviderName.setText(sharedPreferences.getString("DefaultProviderName", ""))
                    itemProviderEmail.setText(sharedPreferences.getString("DefaultProviderEmail", ""))
                    itemProviderPhoneNumber.setText(sharedPreferences.getString("DefaultProviderPhoneNumber", ""))
                }
            }

            binding.apply {
                loadFromFileBtn.setOnClickListener {
                    openFile(Uri.parse("/"))
                }
            }
            binding.saveAction.setOnClickListener {
                addNewItem()
            }
        }
    }

    /**
     * Called before fragment is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        // Hide keyboard.
        val inputMethodManager = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as
                InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
        _binding = null
    }
}
