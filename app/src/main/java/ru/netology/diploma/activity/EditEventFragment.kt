package ru.netology.diploma.activity

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.diploma.R
import ru.netology.diploma.databinding.FragmentEditEventBinding
import ru.netology.diploma.dto.AttachmentType
import ru.netology.diploma.dto.EventType
import ru.netology.diploma.util.AndroidUtils
import ru.netology.diploma.util.Constants
import ru.netology.diploma.util.EventDealtWith
import ru.netology.diploma.util.checkMediaType
import ru.netology.diploma.util.createFileFromInputStream
import ru.netology.diploma.util.formatDateTime
import ru.netology.diploma.util.getInputStreamFromUri
import ru.netology.diploma.util.load
import ru.netology.diploma.viewmodel.EventViewModel
import ru.netology.diploma.viewmodel.PostViewModel
import java.util.Calendar

@AndroidEntryPoint
class EditEventFragment : Fragment() {

    private val viewModel: EventViewModel by activityViewModels()
    private val viewModelPost: PostViewModel by activityViewModels()

    private val photoResultContract =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data
                    ?: return@registerForActivityResult
                val file = uri.toFile()
                viewModel.setAttachment(uri, file, AttachmentType.IMAGE)
            }
        }

    private val pickFileResultContract =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data ?: return@registerForActivityResult
                val file = if (uri.scheme == "file") {
                    uri.toFile()
                } else {
                    getInputStreamFromUri(context, uri)?.let { inputStream ->
                        createFileFromInputStream(inputStream)
                    }
                }

                val path = uri.path
                val type = path?.let { it1 -> checkMediaType(it1) }!!
                if (file != null) {
                    viewModel.setAttachment(uri, file, type)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEditEventBinding.inflate(
            inflater,
            container,
            false
        )

        val event = EventDealtWith.get()
        viewModel.setEventToEdit(event)

        binding.edit.requestFocus()
        binding.edit.setText(event.content)
        binding.edit.movementMethod = ScrollingMovementMethod()

        val toolbar: Toolbar = binding.toolbar

        toolbar.inflateMenu(R.menu.menu_edit)


        if (binding.radioButtonOnline.isChecked) {
            viewModel.setEventFormat(EventType.ONLINE)
        } else viewModel.setEventFormat(EventType.OFFLINE)

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_button_online -> {viewModel.setEventFormat(EventType.ONLINE)}
                R.id.radio_button_offline -> {viewModel.setEventFormat(EventType.OFFLINE)}
            }
        }


        binding.chooseDate.text = event.datetime?.let { it1 -> formatDateTime(it1) }

        val calendar = Calendar.getInstance()
        fun showDateTimePickerDialog() {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val datePickerDialog = DatePickerDialog(requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val timePickerDialog = TimePickerDialog(requireContext(),
                        { _, selectedHour, selectedMinute ->
                            val selectedDateTime = String.format("%02d.%02d.%04d, %02d:%02d", selectedDay, selectedMonth + 1, selectedYear, selectedHour, selectedMinute)
                            viewModel.setEventDateTime (selectedDateTime)
                        }, hour, minute, true)

                    timePickerDialog.show()
                }, year, month, day)

            datePickerDialog.show()
        }

        binding.chooseDate.setOnClickListener {
            showDateTimePickerDialog()
        }

        viewModel.eventDateTime.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.chooseDate.text = formatDateTime(it)
            } else binding.chooseDate.text = event.datetime?.let { it1 -> formatDateTime(it1) }
        }


        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.save -> {

                    val dateTime = viewModel.eventDateTime.value ?: event.datetime
                    val format = viewModel.eventFormat.value!!
                    val speakers = viewModelPost.userChosen.value ?: viewModelPost.postToEdit.value?.mentionIds
                    val finalSpeakers = speakers ?: emptyList()

                    val eventEdited = viewModel.eventToEdit.value?.copy(content = binding.edit.text.toString(), datetime = dateTime, type = format, speakerIds = finalSpeakers)
                    if (eventEdited != null) {
                        viewModel.editEvent(eventEdited, viewModelPost.coords.value)
                    }
                    AndroidUtils.hideKeyboard(requireView())
                    true
                }

                R.id.cancel -> findNavController().navigateUp()
                else -> false
            }
        }

        binding.linkIcon.setOnClickListener {

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            val pickIntent = Intent.createChooser(intent, context?.getString(R.string.select_file))
            pickFileResultContract.launch(pickIntent)

        }


        binding.takePhoto.setOnClickListener {
            ImagePicker.Builder(this)
                .crop()
                .cameraOnly()
                .maxResultSize(2048, 2048)
                .createIntent { photoResultContract.launch(it) }
        }

        binding.pickPhoto.setOnClickListener {
            ImagePicker.Builder(this)
                .crop()
                .galleryOnly()
                .compress(2048)
                .createIntent { photoResultContract.launch(it) }
        }

        binding.photoPreview.visibility = View.GONE
        binding.videoPreview.visibility = View.GONE
        binding.audioPreview.visibility = View.GONE

        viewModel.eventToEdit.observe(viewLifecycleOwner) { event1 ->
            if (viewModel.attachment.value == null) {

                when (event1.attachment?.type) {
                    AttachmentType.IMAGE -> {
                        binding.photoPreview.isVisible = true
                        event1.attachment.url?.let { it1 -> binding.photoPreview.load(it1) }
                    }

                    AttachmentType.VIDEO -> {
                        binding.videoPreview.isVisible = true
                        binding.videoPreview.setVideoPath(event1.attachment.url)
                        binding.videoPreview.setOnPreparedListener { mediaPlayer ->
                            mediaPlayer?.setVolume(0F, 0F)
                            mediaPlayer?.isLooping = true
                            binding.videoPreview.start()
                        }

                    }

                    AttachmentType.AUDIO -> {
                        binding.audioPreview.isVisible = true
                    }

                    else -> Unit
                }
            }
        }

        viewModel.attachment.observe(viewLifecycleOwner) {attachmentModel ->
            if (attachmentModel != null) {
                when (attachmentModel.type) {
                    AttachmentType.IMAGE -> {
                        binding.videoPreview.visibility = View.GONE
                        binding.audioPreview.visibility = View.GONE

                        binding.photoPreview.isVisible = true
                        binding.photoPreview.setImageURI(attachmentModel.uri)
                    }

                    AttachmentType.VIDEO -> {
                        binding.photoPreview.visibility = View.GONE
                        binding.audioPreview.visibility = View.GONE

                        binding.videoPreview.isVisible = true
                        binding.videoPreview.setVideoURI(attachmentModel.uri)
                        binding.videoPreview.setOnPreparedListener { mediaPlayer ->
                            mediaPlayer?.setVolume(0F, 0F)
                            mediaPlayer?.isLooping = true
                            binding.videoPreview.start()
                        }

                    }
                    AttachmentType.AUDIO -> {
                        binding.videoPreview.visibility = View.GONE
                        binding.photoPreview.visibility = View.GONE

                        binding.audioPreview.isVisible = true
                    }
                    else-> Unit
                }
            }
        }

        binding.removeAttachment.setOnClickListener{
            viewModel.clearAttachment()
            viewModel.clearAttachmentEditEvent()
            binding.photoPreview.visibility = View.GONE
            binding.videoPreview.visibility = View.GONE
            binding.audioPreview.visibility = View.GONE
            val toast = Toast.makeText(context, R.string.remove, Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }

        binding.removeLocation.setOnClickListener{
            viewModel.clearLocationEditEvent()
            val toast = Toast.makeText(context, R.string.remove, Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }

        binding.removeSpeakers.setOnClickListener{
            viewModelPost.clearUserChosen()
            viewModel.clearSpeakersEdit()
            val toast = Toast.makeText(context, R.string.remove, Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }


        binding.choosePlace.setOnClickListener{
            findNavController().navigate(R.id.action_editEventFragment_to_mapsFragment)
        }

        requireActivity().onBackPressedDispatcher.addCallback(this)
        {
            (activity as MainActivity).getSharedPreferences(
                Constants.DRAFT_PREF_NAME,
                Context.MODE_PRIVATE
            ).edit().apply {
                putString(Constants.DRAFT_KEY, binding.edit.text.toString())
                apply()
            }
            findNavController().navigateUp()
        }

        viewModel.eventCreated.observe(viewLifecycleOwner) {
            viewModel.clearAttachment()
            viewModel.clearEventDateTime()
            viewModelPost.clearCoords()
            viewModel.speaker = false
            findNavController().navigate(R.id.action_editEventFragment_to_allEventsFragment)
        }

        viewModel.eventCreatedError.observe(viewLifecycleOwner) {

            Snackbar.make(binding.scrollView, "", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.edit)
                .setTextMaxLines(3)
                .setText(R.string.error_loading)
                .setBackgroundTint(Color.rgb(0, 102, 255))
                .show()

        }
        return binding.root
    }
}