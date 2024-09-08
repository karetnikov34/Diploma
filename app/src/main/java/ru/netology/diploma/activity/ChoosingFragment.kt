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
import ru.netology.diploma.adapter.UserAdapter.Companion.selectedItems
import ru.netology.diploma.databinding.FragmentAllLikersBinding
import ru.netology.diploma.dto.UserResponse
import ru.netology.diploma.viewmodel.EventViewModel
import ru.netology.diploma.viewmodel.PostViewModel
import ru.netology.diploma.viewmodel.UserViewModel

@AndroidEntryPoint
class ChoosingFragment: Fragment() {

    private val viewModelUser: UserViewModel by activityViewModels()
    private val viewModelPost: PostViewModel by activityViewModels()
    private val viewModelEvent: EventViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAllLikersBinding.inflate(
            inflater,
            container,
            false
        )


        binding.likers.text = if (viewModelEvent.speaker) context?.getString(R.string.choose_speakers) else context?.getString(
            R.string.choose_users)
        binding.checkbox.isVisible = true
        UserAdapter.choosing = true

        val adapter = UserAdapter(object : OnInteractionListenerUser {
            override fun show(user: UserResponse) {}

            override fun choose(user: UserResponse) {
                viewModelUser.choosing(user)
            }
        })

        binding.recyclerList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelUser.userList.collectLatest{
                    adapter.submitList(it)
                    selectedItems.clear()
                }
            }
        }

        binding.checkbox.setOnClickListener {
            val listIds = viewModelUser.uniqueSet.toList()
            viewModelUser.clearChoosing()
            viewModelPost.setUserChosen(listIds)
            viewModelEvent.speaker = false
            findNavController().navigateUp()
        }

        binding.back.setOnClickListener {
            viewModelUser.clearChoosing()
            viewModelEvent.speaker = false
            findNavController().navigateUp()
        }


        viewModelUser.dataState.observe(viewLifecycleOwner) { feedModelState ->
            binding.progress.isVisible = feedModelState.loading
            binding.errorGroup.isVisible = feedModelState.error
        }

        binding.retryButton.setOnClickListener {
            viewModelUser.loadUsers()
        }

        binding.swiperefresh.setOnRefreshListener {
            viewModelUser.loadUsers()
            binding.swiperefresh.isRefreshing = false
        }


        return binding.root
    }
}