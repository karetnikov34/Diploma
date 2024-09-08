package ru.netology.diploma.activity

import android.app.Activity
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
import ru.netology.diploma.databinding.FragmentEditPostBinding
import ru.netology.diploma.dto.AttachmentType
import ru.netology.diploma.util.AndroidUtils
import ru.netology.diploma.util.Constants
import ru.netology.diploma.util.PostDealtWith
import ru.netology.diploma.util.createFileFromInputStream
import ru.netology.diploma.util.getInputStreamFromUri
import ru.netology.diploma.util.load
import ru.netology.diploma.viewmodel.PostViewModel
import ru.netology.diploma.viewmodel.UserViewModel

@AndroidEntryPoint
class EditPostFragment : Fragment() {

    private val viewModelPost: PostViewModel by activityViewModels()
    private val viewModelUser: UserViewModel by activityViewModels()

    private val photoResultContract =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data
                    ?: return@registerForActivityResult
                val file = uri.toFile()
                viewModelPost.setAttachment(uri, file, AttachmentType.IMAGE)
            }
        }

    private val pickAudioResultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val uri = it.data?.data ?: return@registerForActivityResult
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

    private val pickVideoResultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val uri = it.data?.data ?: return@registerForActivityResult
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEditPostBinding.inflate(
            inflater,
            container,
            false
        )

        val post = PostDealtWith.get()
        viewModelPost.setPostToEdit(post)

        binding.edit.requestFocus()
        binding.edit.setText(post.content)
        binding.edit.movementMethod = ScrollingMovementMethod()


        val toolbar: Toolbar = binding.toolbar

        toolbar.inflateMenu(R.menu.menu_edit)

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.save -> {

                    val mentioned = viewModelPost.userChosen.value ?: viewModelPost.postToEdit.value?.mentionIds
                    val finalMentioned = mentioned ?: emptyList()

                    val postEdited =
                        viewModelPost.postToEdit.value?.copy(content = binding.edit.text.toString(), mentionIds = finalMentioned)
                    if (postEdited != null) {
                        viewModelPost.edit(postEdited)
                    }
                    AndroidUtils.hideKeyboard(requireView())
                    true
                }
                R.id.cancel -> {
                    viewModelPost.clearAttachment()
                    viewModelPost.clearAttachmentEdit()
                    viewModelPost.clearLocationEdit()
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
            val pickIntent = Intent.createChooser (intent,context?.getString(R.string.select_file))
            pickAudioResultContract.launch(pickIntent)

        }

        binding.videoIcon.setOnClickListener {

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "video/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            val pickIntent = Intent.createChooser (intent,context?.getString(R.string.select_file))
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

        binding.photoPreview.visibility = View.GONE
        binding.videoPreview.visibility = View.GONE
        binding.audioPreview.visibility = View.GONE

        viewModelPost.postToEdit.observe(viewLifecycleOwner) { post ->
            if (viewModelPost.attachment.value == null) {

                when (post.attachment?.type) {
                    AttachmentType.IMAGE -> {
                        binding.photoPreview.isVisible = true
                        post.attachment.url?.let { it1 -> binding.photoPreview.load(it1) }
                    }

                    AttachmentType.VIDEO -> {
                        binding.videoPreview.isVisible = true
                        binding.videoPreview.setVideoPath(post.attachment.url)
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

        viewModelPost.attachment.observe(viewLifecycleOwner) { attachmentModel ->
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
            viewModelPost.clearAttachment()
            viewModelPost.clearAttachmentEdit()
            binding.photoPreview.visibility = View.GONE
            binding.videoPreview.visibility = View.GONE
            binding.audioPreview.visibility = View.GONE
            val toast = Toast.makeText(context, R.string.remove, Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }

        binding.removeLocation.setOnClickListener{
            viewModelPost.clearLocationEdit()
            val toast = Toast.makeText(context, R.string.remove, Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }

        binding.removeMentioned.setOnClickListener{
            viewModelUser.clearChoosing()
            viewModelPost.clearUserChosen()
            viewModelPost.clearMentionedEdit()
            val toast = Toast.makeText(context, R.string.remove, Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }

        binding.chooseUsers.setOnClickListener{
            findNavController().navigate(R.id.action_editPostFragment_to_choosingFragment)
        }


        binding.choosePlace.setOnClickListener{
            findNavController().navigate(R.id.action_editPostFragment_to_mapsFragment)
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

        viewModelPost.postCreated.observe(viewLifecycleOwner)
        {
            findNavController().navigate(R.id.action_editPostFragment_to_allPostsFragment)
        }

        viewModelPost.postCreatedError.observe(viewLifecycleOwner)
        {

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