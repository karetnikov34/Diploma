package ru.netology.diploma.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toFile
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.diploma.R
import ru.netology.diploma.databinding.FragmentNewEventBinding
import ru.netology.diploma.dto.AttachmentType
import ru.netology.diploma.util.AndroidUtils
import ru.netology.diploma.util.Constants
import ru.netology.diploma.util.createFileFromInputStream
import ru.netology.diploma.util.formatDateTime
import ru.netology.diploma.util.getInputStreamFromUri
import ru.netology.diploma.viewmodel.EventViewModel
import ru.netology.diploma.viewmodel.PostViewModel

@AndroidEntryPoint
class NewEventFragment : Fragment() {

    private val viewModelEvent: EventViewModel by activityViewModels()
    private val viewModelPost: PostViewModel by activityViewModels()

    private val photoResultContract =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data ?: return@registerForActivityResult
                val file = uri.toFile()
                viewModelEvent.setAttachment(uri, file, AttachmentType.IMAGE)
            }
        }

    private val pickAudioResultContract =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data ?: return@registerForActivityResult
                val fileSize = DocumentFile.fromSingleUri(requireContext(), uri)?.length()
                if (fileSize != null && fileSize > 15000000) {
                    Toast.makeText(context, R.string.max_file_size, Toast.LENGTH_LONG).show()
                } else {
                    val file = if (uri.scheme == "file") {
                        uri.toFile()
                    } else {
                        getInputStreamFromUri(context, uri)?.let { inputStream ->
                            createFileFromInputStream(inputStream)
                        }
                    }
                    if (file != null) {
                        viewModelEvent.setAttachment(uri, file, AttachmentType.AUDIO)
                    }
                }
            }
        }

    private val pickVideoResultContract =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data ?: return@registerForActivityResult
                val fileSize = DocumentFile.fromSingleUri(requireContext(), uri)?.length()
                if (fileSize != null && fileSize > 15000000) {
                    Toast.makeText(context, R.string.max_file_size, Toast.LENGTH_LONG).show()
                } else {
                    val file = if (uri.scheme == "file") {
                        uri.toFile()
                    } else {
                        getInputStreamFromUri(context, uri)?.let { inputStream ->
                            createFileFromInputStream(inputStream)
                        }
                    }
                    if (file != null) {
                        viewModelEvent.setAttachment(uri, file, AttachmentType.VIDEO)
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewEventBinding.inflate(
            inflater,
            container,
            false
        )

        binding.edit.requestFocus()

        val toolbar: Toolbar = binding.toolbar

        toolbar.inflateMenu(R.menu.save_menu)


        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.save -> {
                    if (viewModelEvent.eventDateTime.value == null) {
                        Snackbar.make(binding.scrollView, "", Snackbar.LENGTH_LONG)
                            .setAnchorView(binding.edit)
                            .setTextMaxLines(3)
                            .setText(R.string.choose_date_of_your_event)
                            .show()
                        false
                    } else {
                        val speakers = viewModelPost.userChosen.value ?: emptyList()
                        viewModelEvent.changeContentAndSave(
                            binding.edit.text.toString(),
                            viewModelPost.coords.value,
                            speakers
                        )
                        AndroidUtils.hideKeyboard(requireView())
                        true
                    }
                }

                R.id.cancel -> {
                    viewModelEvent.clearAttachment()
                    viewModelEvent.clearEventDateTime()
                    viewModelPost.clearCoords()
                    viewModelPost.clearUserChosen()
                    findNavController().navigateUp()
                }

                else -> false
            }
        }

        binding.audioIcon.setOnClickListener {

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            val pickIntent = Intent.createChooser(intent, context?.getString(R.string.select_file))
            pickAudioResultContract.launch(pickIntent)

        }

        binding.videoIcon.setOnClickListener {

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "video/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            val pickIntent = Intent.createChooser(intent, context?.getString(R.string.select_file))
            pickVideoResultContract.launch(pickIntent)

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

        viewModelEvent.attachment.observe(viewLifecycleOwner) { attachmentModel ->
            if (attachmentModel == null) {
                binding.photoContainer.isGone = true
                binding.videoContainer.isGone = true
                binding.audioContainer.isGone = true
                return@observe
            }

            when (attachmentModel.type) {
                AttachmentType.IMAGE -> {
                    binding.photoContainer.isVisible = true
                    binding.photoPreview.setImageURI(attachmentModel.uri)
                }

                AttachmentType.VIDEO -> {
                    binding.videoContainer.isVisible = true
                    binding.videoPreview.setVideoURI(attachmentModel.uri)
                    binding.videoPreview.setOnPreparedListener { mediaPlayer ->
                        mediaPlayer?.setVolume(0F, 0F)
                        mediaPlayer?.isLooping = true
                        binding.videoPreview.start()
                    }

                }

                AttachmentType.AUDIO -> {
                    binding.audioContainer.isVisible = true
                }

                else -> Unit
            }

        }


        binding.removePhoto.setOnClickListener {
            viewModelEvent.clearAttachment()
        }

        binding.removeVideo.setOnClickListener {
            viewModelEvent.clearAttachment()
        }

        binding.removeAudio.setOnClickListener {
            viewModelEvent.clearAttachment()
        }

        binding.chooseSpeakers.setOnClickListener {
            viewModelEvent.speaker = true
            findNavController().navigate(R.id.action_newEventFragment_to_choosingFragment)
        }


        binding.choosePlace.setOnClickListener {
            findNavController().navigate(R.id.action_newEventFragment_to_mapsFragment)
        }

        binding.chooseDate.setOnClickListener {
            BottomSheetDialogue().show(parentFragmentManager, "format_date_time")
        }
        viewModelEvent.eventDateTime.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.chooseDate.text = formatDateTime(it)
            }
        }


        requireActivity().onBackPressedDispatcher.addCallback(this) {
            (activity as MainActivity).getSharedPreferences(
                Constants.DRAFT_PREF_NAME,
                Context.MODE_PRIVATE
            ).edit().apply {
                putString(Constants.DRAFT_KEY, binding.edit.text.toString())
                apply()
            }
            findNavController().navigateUp()
        }

        viewModelEvent.eventCreated.observe(viewLifecycleOwner) {
            viewModelEvent.clearAttachment()
            viewModelEvent.clearEventDateTime()
            viewModelPost.clearCoords()
            viewModelPost.clearUserChosen()
            viewModelEvent.speaker = false
            findNavController().navigate(R.id.action_newEventFragment_to_allEventsFragment)
        }

        viewModelEvent.eventCreatedError.observe(viewLifecycleOwner) {
            Snackbar.make(binding.scrollView, "", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.edit)
                .setTextMaxLines(3)
                .setText(R.string.error_loading)
                .show()
        }

        return binding.root
    }
}