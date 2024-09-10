package ru.netology.diploma.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.netology.diploma.R
import ru.netology.diploma.databinding.FragmentBottomSheetDialogueBinding
import ru.netology.diploma.dto.EventType
import ru.netology.diploma.util.formatDateTime
import ru.netology.diploma.viewmodel.EventViewModel
import java.util.Calendar

class BottomSheetDialogue : BottomSheetDialogFragment() {

    lateinit var binding: FragmentBottomSheetDialogueBinding
    private val viewModelEvent: EventViewModel by activityViewModels()

    override fun getTheme() = R.style.AppBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBottomSheetDialogueBinding.bind(
            inflater.inflate(
                R.layout.fragment_bottom_sheet_dialogue,
                container,
                false
            )
        )

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.let {
            val bottomSheet =
                it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            val behavior = BottomSheetBehavior.from(bottomSheet)

            behavior.peekHeight = bottomSheet.height
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        if (binding.radioButtonOnline.isChecked) {
            viewModelEvent.setEventFormat(EventType.ONLINE)
        } else viewModelEvent.setEventFormat(EventType.OFFLINE)

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_button_online -> {
                    viewModelEvent.setEventFormat(EventType.ONLINE)
                }

                R.id.radio_button_offline -> {
                    viewModelEvent.setEventFormat(EventType.OFFLINE)
                }
            }
        }

        val calendar = Calendar.getInstance()
        fun showDateTimePickerDialog() {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val timePickerDialog = TimePickerDialog(
                        requireContext(),
                        { _, selectedHour, selectedMinute ->
                            val selectedDateTime = String.format(
                                "%02d.%02d.%04d, %02d:%02d",
                                selectedDay,
                                selectedMonth + 1,
                                selectedYear,
                                selectedHour,
                                selectedMinute
                            )
                            viewModelEvent.setEventDateTime(selectedDateTime)
                        }, hour, minute, true
                    )
                    timePickerDialog.show()
                }, year, month, day
            )

            datePickerDialog.show()
        }

        binding.chooseDateTime.setOnClickListener {
            showDateTimePickerDialog()
        }

        viewModelEvent.eventDateTime.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.chooseDateTime.text = formatDateTime(it)
            }
        }
    }
}