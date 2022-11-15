package com.example.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.inventory.databinding.FragmentSettingBinding

class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val PREFS_FILE = "Setting_Key"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val sharedPreferences = EncryptedSharedPreferences.create(
            requireContext(),
            PREFS_FILE,
            MasterKey.Builder(requireContext()).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        _binding = FragmentSettingBinding.inflate(inflater, container, false)

        binding.defaultValues.setOnClickListener {

            binding.linearLayout.visibility = if (binding.linearLayout.visibility == LinearLayout.VISIBLE) {
                LinearLayout.INVISIBLE
            }
            else {
                LinearLayout.VISIBLE
            }
        }
        
        binding.apply {
            hideSensitiveData.isChecked = sharedPreferences.getBoolean("HideSensitiveData", false)
            forbidSendingData.isChecked = sharedPreferences.getBoolean("ForbidSensitiveData", false)
            defaultValues.isChecked = sharedPreferences.getBoolean("DefaultValues", false)
            defaultProviderName.setText(sharedPreferences.getString("DefaultProviderName", ""))
            defaultProviderEmail.setText(sharedPreferences.getString("DefaultProviderEmail", ""))
            defaultProviderPhoneNumber.setText(sharedPreferences.getString("DefaultProviderPhoneNumber", ""))

            btnSaveSetting.setOnClickListener {
                sharedPreferences.edit().apply {
                    putBoolean("HideSensitiveData", hideSensitiveData.isChecked)
                    putBoolean("ForbidSensitiveData", forbidSendingData.isChecked)
                    putBoolean("DefaultValues", defaultValues.isChecked)
                    putString("DefaultProviderName", defaultProviderName.text.toString())
                    putString("DefaultProviderEmail", defaultProviderEmail.text.toString())
                    putString("DefaultProviderPhoneNumber", defaultProviderPhoneNumber.text.toString())
                }.apply()
                val action = SettingFragmentDirections.actionSettingFragmentToItemListFragment()
                findNavController().navigate(action)
            }
        }

        if (binding.defaultValues.isChecked ) {
            binding.linearLayout.visibility = LinearLayout.VISIBLE
        }
        else {
            binding.linearLayout.visibility = LinearLayout.INVISIBLE
        }

        return binding.root
    }

}