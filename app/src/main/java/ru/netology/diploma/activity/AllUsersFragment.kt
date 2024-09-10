package ru.netology.diploma.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.diploma.R
import ru.netology.diploma.adapter.OnInteractionListenerUser
import ru.netology.diploma.adapter.UserAdapter
import ru.netology.diploma.databinding.FragmentAllUsersBinding
import ru.netology.diploma.dto.UserResponse
import ru.netology.diploma.util.UserDealtWith
import ru.netology.diploma.util.loadCircle
import ru.netology.diploma.viewmodel.AuthViewModel
import ru.netology.diploma.viewmodel.PostViewModel
import ru.netology.diploma.viewmodel.UserViewModel

@AndroidEntryPoint
class AllUsersFragment: Fragment() {

    private val viewModelUser: UserViewModel by activityViewModels()
    private val viewModelAuth: AuthViewModel by activityViewModels()
    private val viewModelPost: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAllUsersBinding.inflate(
            inflater,
            container,
            false
        )

        UserAdapter.choosing = false

        val adapter = UserAdapter(object : OnInteractionListenerUser {
            override fun show(user: UserResponse) {
//                if (viewModelAuth.authenticated) {
                    UserDealtWith.saveUserDealtWith(user)
                    findNavController().navigate(R.id.action_allUsersFragment_to_oneUserCardFragment)
//                } else {
//                    signInDialog()
//                }
            }
            override fun choose(user: UserResponse) {}
        })

        binding.recyclerList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelUser.userList.collectLatest{
                    adapter.submitList(it)
                }
            }
        }

        val id = if (viewModelAuth.authenticated) { viewModelAuth.authenticatedId } else 0
        if (id != 0) {viewModelPost.getUserById(id)}
        val user = viewModelPost.userList.value
        if (user != null) {
            val urlAvatar = "${user.avatar}"
            binding.avatarUser.loadCircle(urlAvatar)
            binding.avatarUser.setOnClickListener {
                UserDealtWith.saveUserDealtWith(user)
                findNavController().navigate(R.id.action_allUsersFragment_to_oneUserCardFragment)
            }
        }

        binding.home.setOnClickListener {
            findNavController().navigate(R.id.mainFragment)
        }

        viewModelUser.dataState.observe(viewLifecycleOwner) { feedModelState ->
            binding.progress.isVisible = feedModelState.loading
            binding.errorGroup.isVisible = feedModelState.error
        }

        binding.retryButton.setOnClickListener {
            binding.errorGroup.isVisible = false
            viewModelUser.loadUsers()
        }

        binding.swiperefresh.setOnRefreshListener {
            viewModelUser.loadUsers()
            binding.swiperefresh.isRefreshing = false
        }


        return binding.root
    }

//    private fun signInDialog() {
//        val listener = DialogInterface.OnClickListener{ _, which ->
//            when(which) {
//                DialogInterface.BUTTON_POSITIVE -> findNavController().navigate(R.id.authSignInFragment)
//            }
//        }
//        val dialog = AlertDialog.Builder(context)
//            .setCancelable(false)
//            .setTitle(R.string.not_authorized)
//            .setMessage(R.string.sign_in_account)
//            .setPositiveButton(R.string.yes, listener)
//            .setNegativeButton(R.string.no, listener)
//            .create()
//        dialog.show()
//    }
}