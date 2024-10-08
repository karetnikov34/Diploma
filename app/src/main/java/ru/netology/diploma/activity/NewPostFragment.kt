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
import ru.netology.diploma.databinding.FragmentNewPostBinding
import ru.netology.diploma.dto.AttachmentType
import ru.netology.diploma.util.AndroidUtils
import ru.netology.diploma.util.Constants
import ru.netology.diploma.util.createFileFromInputStream
import ru.netology.diploma.util.getInputStreamFromUri
import ru.netology.diploma.viewmodel.PostViewModel

@AndroidEntryPoint
class NewPostFragment : Fragment() {

    private val viewModelPost: PostViewModel by activityViewModels()

    private val photoResultContract =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data ?: return@registerForActivityResult
                val file = uri.toFile()
                viewModelPost.setAttachment(uri, file, AttachmentType.IMAGE)
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
                        viewModelPost.setAttachment(uri, file, AttachmentType.AUDIO)
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
                        viewModelPost.setAttachment(uri, file, AttachmentType.VIDEO)
                    }
                }
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewPostBinding.inflate(
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
                    val mentionedIds = viewModelPost.userChosen.value ?: emptyList()
                    viewModelPost.changeContentAndSave(binding.edit.text.toString(), mentionedIds)
                    AndroidUtils.hideKeyboard(requireView())
                    true
                }

                R.id.cancel -> {
                    viewModelPost.clearAttachment()
                    viewModelPost.clearUserChosen()
                    viewModelPost.clearCoords()
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

        viewModelPost.attachment.observe(viewLifecycleOwner) { attachmentModel ->
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
            viewModelPost.clearAttachment()
        }

        binding.removeVideo.setOnClickListener {
            viewModelPost.clearAttachment()
        }

        binding.removeAudio.setOnClickListener {
            viewModelPost.clearAttachment()
        }

        binding.chooseUsers.setOnClickListener {
            findNavController().navigate(R.id.action_newPostFragment_to_choosingFragment)
        }


        binding.choosePlace.setOnClickListener {
            findNavController().navigate(R.id.action_newPostFragment_to_mapsFragment)
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

        viewModelPost.postCreated.observe(viewLifecycleOwner) {
            viewModelPost.clearAttachment()
            viewModelPost.clearUserChosen()
            findNavController().navigateUp()
        }

        viewModelPost.postCreatedError.observe(viewLifecycleOwner) {
            Snackbar.make(binding.scrollView, "", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.edit)
                .setTextMaxLines(3)
                .setText(R.string.error_loading)
                .show()
        }

        return binding.root
    }
}