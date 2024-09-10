package ru.netology.diploma.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.diploma.R
import ru.netology.diploma.databinding.FragmentNewJobBinding
import ru.netology.diploma.util.AndroidUtils
import ru.netology.diploma.util.JobDealtWith
import ru.netology.diploma.util.formatDateTimeJob
import ru.netology.diploma.util.formatDateTimeJobBinding
import ru.netology.diploma.viewmodel.UserViewModel
import java.util.Calendar

@AndroidEntryPoint
class EditJobFragment : Fragment() {

    private val viewModelUser: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewJobBinding.inflate(
            inflater,
            container,
            false
        )

        val job = JobDealtWith.get()

        with(binding) {
            companyJob.setText(job.name)
            positionJob.setText(job.position)
            startJob.text = formatDateTimeJobBinding(job.start)
            finishJob.text = job.finish?.let { formatDateTimeJobBinding(it) } ?: ""
            linkJob.setText(job.link)
            createJob.setText(R.string.change)
        }

        with(binding) {
            createJob.setOnClickListener {
                if (companyJob.text.isNullOrBlank() || positionJob.text.isNullOrBlank() || startJob.text.isNullOrBlank()) {
                    Snackbar.make(binding.companyJob, "", Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.startJob)
                        .setTextMaxLines(3)
                        .setText(R.string.fields_must_be_filled_in)
                        .show()
                } else {
                    val company = binding.companyJob.text.toString()
                    val position = binding.positionJob.text.toString()
                    val start = formatDateTimeJob(binding.startJob.text.trim().toString())
                    val finish = if (binding.finishJob.text?.isNotBlank() == true) {
                        formatDateTimeJob(binding.finishJob.text.trim().toString())
                    } else null
                    val link = if (binding.linkJob.text?.isNotBlank() == true) {
                        binding.linkJob.text?.trim()
                        binding.linkJob.text.toString()
                    } else null
                    viewModelUser.editJob(job.id, company, position, start, finish, link)
                    AndroidUtils.hideKeyboard(requireView())

                }
            }
        }

        binding.startJob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = String.format(
                        "%02d.%02d.%04d",
                        selectedDay,
                        selectedMonth + 1,
                        selectedYear
                    )
                    binding.startJob.text = selectedDate
                }, year, month, day
            )
            datePickerDialog.show()
        }

        binding.finishJob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = String.format(
                        "%02d.%02d.%04d",
                        selectedDay,
                        selectedMonth + 1,
                        selectedYear
                    )
                    binding.finishJob.text = selectedDate
                }, year, month, day
            )
            datePickerDialog.show()
        }

        binding.back.setOnClickListener {
            findNavController().navigateUp()
        }

        viewModelUser.jobCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        viewModelUser.error.observe(viewLifecycleOwner) {
            Snackbar.make(binding.companyJob, "", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.companyJob)
                .setTextMaxLines(3)
                .setText(R.string.error_loading)
                .show()
        }

        return binding.root
    }
}