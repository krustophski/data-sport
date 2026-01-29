package com.krustophski.data.sport

import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krustophski.data.sport.databinding.FragmentBiometricsPreferenceBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BiometricsPreferenceFragment : Fragment() {
    private val logTag = "BiometricsPreferences"

    companion object {
        fun newInstance() = BiometricsPreferenceFragment()
    }

    private lateinit var viewModel: BiometricsViewModel
    private lateinit var binding: FragmentBiometricsPreferenceBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        viewModel =
            BiometricsViewModel(
                GoogleFitApiService(requireActivity()),
                PreferenceManager.getDefaultSharedPreferences(requireContext())
            )
        binding = FragmentBiometricsPreferenceBinding.inflate(inflater, container, false)
        binding.viewmodel = viewModel

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val keyUnits = getString(R.string.preference_key_system_of_measurement)
        val keySex = getString(R.string.preference_key_biometrics_user_sex)
        val keyHeight = getString(R.string.preference_key_biometrics_user_height)
        val keyWeight = getString(R.string.preference_key_biometrics_user_weight)
        prefs.edit().apply {
            if (prefs.getString(keyUnits, null).isNullOrBlank()) {
                putString(keyUnits, "2")
            }
            if (prefs.getString(keySex, null).isNullOrBlank()) {
                putString(keySex, UserSexEnum.MALE.name)
            }
            if (prefs.getString(keyHeight, null).isNullOrBlank()) {
                putString(keyHeight, "172")
            }
            if (prefs.getString(keyWeight, null).isNullOrBlank()) {
                putString(keyWeight, "67")
            }
            commit()
        }

        activity?.title = ""

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<FloatingActionButton>(R.id.fab).apply {
            visibility = GONE
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateGoogleFitBiometrics()
        }
    }

    override fun onStop() {
        super.onStop()
        requireActivity().findViewById<FloatingActionButton>(R.id.fab).apply {
            visibility = VISIBLE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(logTag, "onViewCreated")
    }
}
