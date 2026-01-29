package com.krustophski.data.sport.preferences

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.krustophski.data.sport.R
import com.krustophski.data.sport.ExportTripWorker
import com.krustophski.data.sport.TripsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AdvancedPreferencesFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var tripsRepository: TripsRepository

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.advanced_preferences, rootKey)
        findPreference<Preference>(getString(R.string.preference_key_advanced_export_sensors_log))
            ?.setOnPreferenceClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    val latestTrip = tripsRepository.getNewest()
                    val tripId = latestTrip?.id
                    if (tripId == null) {
                        Toast.makeText(
                            requireContext(),
                            "No trips found to export.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    WorkManager.getInstance(requireContext())
                        .enqueue(
                            OneTimeWorkRequestBuilder<ExportTripWorker>()
                                .setInputData(
                                    workDataOf(
                                        "tripId" to tripId,
                                        "fileType" to "xlsx"
                                    )
                                )
                                .build()
                        )
                    Toast.makeText(
                        requireContext(),
                        "Export started. File will appear in Downloads.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<Toolbar>(R.id.preferences_toolbar).title =
            "Settings: Advanced"
    }
}
