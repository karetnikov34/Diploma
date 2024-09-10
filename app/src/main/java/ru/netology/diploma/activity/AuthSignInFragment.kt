package ru.netology.diploma.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.diploma.R
import ru.netology.diploma.databinding.FragmentAuthSignInBinding
import ru.netology.diploma.viewmodel.SignInViewModel

@AndroidEntryPoint
class AuthSignInFragment : Fragment() {

    private val viewModelSignIn: SignInViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAuthSignInBinding.inflate(
            inflater,
            container,
            false
        )

        binding.signIn.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val login = binding.loginEditText.text.toString()
                val password = binding.passwordEditText.text.toString()
                binding.signIn.isEnabled = !(login.isEmpty() || password.isEmpty())
            }
        }

        with(binding) {
            loginEditText.addTextChangedListener(textWatcher)
            passwordEditText.addTextChangedListener(textWatcher)
        }

        with(binding) {

            signIn.setOnClickListener {
                viewModelSignIn.checkAndSetAuth(
                    loginEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }
        }

        viewModelSignIn.response.observe(viewLifecycleOwner) {
            findNavController().navigate(R.id.mainFragment)
        }

        viewModelSignIn.error.observe(viewLifecycleOwner) {
            Snackbar.make(binding.signIn, "", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.loginEditText)
                .setTextMaxLines(3)
                .setText(R.string.error_wrong_login_or_password)
                .show()
        }

        return binding.root
    }
}