package ru.netology.diploma.activity

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.diploma.R
import ru.netology.diploma.databinding.FragmentAuthSignUpBinding
import ru.netology.diploma.dto.AttachmentType
import ru.netology.diploma.model.AttachmentModel
import ru.netology.diploma.viewmodel.SignUpViewModel
import java.io.File
import java.util.Locale

@AndroidEntryPoint
class AuthSignUpFragment : Fragment() {

    private val viewModelSignUp: SignUpViewModel by activityViewModels()

    private val photoResultContract =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data ?: return@registerForActivityResult
                val file = uri.toFile()
                viewModelSignUp.setPhoto(uri, file, AttachmentType.IMAGE)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAuthSignUpBinding.inflate(
            inflater,
            container,
            false
        )

        binding.signUp.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val username = binding.nameEditText.text.toString()
                val login = binding.loginEditText.text.toString()
                val password = binding.passwordEditText.text.toString()
                val passwordConfirm = binding.passwordConfirmEditText.text.toString()

                binding.signUp.isEnabled =
                    !(username.isEmpty() || login.isEmpty() || password.isEmpty() || password.isEmpty() || password != passwordConfirm)
            }
        }

        with(binding) {
            nameEditText.addTextChangedListener(textWatcher)
            loginEditText.addTextChangedListener(textWatcher)
            passwordEditText.addTextChangedListener(textWatcher)
            passwordConfirmEditText.addTextChangedListener(textWatcher)
        }

        with(binding) {
            signUp.setOnClickListener {
                if (viewModelSignUp.photo.value == null) {
                    viewModelSignUp.registerAndSetAuth(
                        loginEditText.text.toString(),
                        passwordEditText.text.toString(),
                        nameEditText.text.toString()
                    )
                } else {
                    if (isImageValid(viewModelSignUp.photo.value!!)) {
                        viewModelSignUp.registerWithAvatarAndSetAuth(
                            loginEditText.text.toString(),
                            passwordEditText.text.toString(),
                            nameEditText.text.toString()
                        )
                    } else {
                        Snackbar.make(binding.scrollView, "", Snackbar.LENGTH_LONG)
                            .setAnchorView(binding.signUp)
                            .setTextMaxLines(3)
                            .setText(R.string.only_jpeg_png_with_maximum_size_2048_2048)
                            .show()
                    }
                }
            }
        }

        viewModelSignUp.response.observe(viewLifecycleOwner) {
            findNavController().navigate(R.id.mainFragment)
        }

        viewModelSignUp.error.observe(viewLifecycleOwner) {
            Snackbar.make(binding.signUp, "", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.signUp)
                .setTextMaxLines(3)
                .setText(R.string.user_is_already_registered)
                .show()
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
                .maxResultSize(2048, 2048)
                .createIntent { photoResultContract.launch(it) }
        }

        binding.removePhoto.setOnClickListener {
            viewModelSignUp.clearPhoto()
        }

        viewModelSignUp.photo.observe(viewLifecycleOwner) {
            if (it == null) {
                binding.avatarPreview.setImageResource(R.drawable.ic_account_box_120)
                return@observe
            }
            binding.avatarPreview.setImageURI(it.uri)
        }

        return binding.root
    }

    private fun isImageValid(attachmentModel: AttachmentModel): Boolean {
        val allowedFormats = listOf("jpg", "jpeg", "png")
        val maxImageSize = 2048
        if (attachmentModel.type != AttachmentType.IMAGE) {
            return false
        }
        val filePath = attachmentModel.uri.path!!
        val imageFile = File(filePath)
        val fileExtension = imageFile.extension.lowercase(Locale.ROOT)
        if (fileExtension !in allowedFormats) {
            return false
        }

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imageFile.absolutePath, options)
        val imageWidth = options.outWidth
        val imageHeight = options.outHeight
        if (imageWidth > maxImageSize || imageHeight > maxImageSize) {
            return false
        }
        return true
    }
}