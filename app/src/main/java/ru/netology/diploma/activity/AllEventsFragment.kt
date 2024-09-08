package ru.netology.diploma.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.diploma.R
import ru.netology.diploma.adapter.EventAdapter
import ru.netology.diploma.adapter.OnInteractionListenerEvent
import ru.netology.diploma.auth.AppAuth
import ru.netology.diploma.databinding.FragmentAllEventsBinding
import ru.netology.diploma.dto.Event
import ru.netology.diploma.util.EventDealtWith
import ru.netology.diploma.util.UserDealtWith
import ru.netology.diploma.viewmodel.AuthViewModel
import ru.netology.diploma.viewmodel.EventViewModel
import ru.netology.diploma.viewmodel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class AllEventsFragment : Fragment() {

    private val viewModelEvent: EventViewModel by activityViewModels()
    private val viewModelAuth: AuthViewModel by activityViewModels()
    private val viewModelPost: PostViewModel by activityViewModels()

    private val mediaObserver = MediaLifecycleObserver()

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAllEventsBinding.inflate(
            inflater,
            container,
            false
        )

        var trackId = -1

        val adapter = EventAdapter(object : OnInteractionListenerEvent {
            override fun like(event: Event) {
                if (viewModelAuth.authenticated) {
                    viewModelEvent.likeEventById(event)
                } else {
                    signInDialog()
                }
            }

            override fun remove(event: Event) {
                viewModelEvent.removeEventById(event.id)
            }

            override fun edit(event: Event) {
                EventDealtWith.saveEventDealtWith(event)
                findNavController().navigate(R.id.action_allEventsFragment_to_editEventFragment)
            }

            override fun showEvent (event: Event) {
                EventDealtWith.saveEventDealtWith(event)
                findNavController().navigate(R.id.action_allEventsFragment_to_oneEventFragment)
            }


            override fun playMusic(event: Event) {
                val thisTrackId = event.id
                if (mediaObserver.player?.isPlaying == true) {
                    mediaObserver.apply {
                        viewModelEvent.updateIsPlayingEvent(event.id, false)
                        viewModelEvent.updatePlayer()
                        stop()
                        if (thisTrackId != trackId ) {
                            trackId = thisTrackId
                            viewModelEvent.updateIsPlayingEvent(event.id, true)
                            event.attachment?.url?.let { play(it) }
                        }
                    }
                } else {
                    mediaObserver.apply {
                        trackId = thisTrackId
                        viewModelEvent.updateIsPlayingEvent(event.id, true)
                        event.attachment?.url?.let { play(it) }
                    }
                }
            }

        }
        )

        mediaObserver.player?.setOnCompletionListener {
            mediaObserver.player?.stop()
            viewModelEvent.updatePlayer()
        }


        val toolbarAll: Toolbar = binding.toolbarPosts

        binding.home.setOnClickListener {
            findNavController().navigate(R.id.mainFragment)
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModelAuth.dataAuth.collect {
                    toolbarAll.invalidateMenu()
                }
            }
        }

        toolbarAll.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)

                menu.let {
                    it.setGroupVisible(R.id.unauthenticated, !viewModelAuth.authenticated)
                    it.setGroupVisible(R.id.authenticated, viewModelAuth.authenticated)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.signin -> {
                        findNavController().navigate(R.id.authSignInFragment)
                        true
                    }

                    R.id.signup -> {
                        findNavController().navigate(R.id.authSignUpFragment)
                        true
                    }

                    R.id.profile -> {
                        val id = if (viewModelAuth.authenticated) { viewModelAuth.authenticatedId } else 0
                        if (id != 0) {viewModelPost.getUserById(id)}
                        val user = viewModelPost.userList.value
                        if (user != null) {
                            UserDealtWith.saveUserDealtWith(user)
                            findNavController().navigate(R.id.action_allEventsFragment_to_oneUserCardFragment)

                        }
                        true
                    }

                    R.id.signout -> {
                        signOutDialog()
                        true
                    }

                    else -> false
                }
        })

        binding.recyclerList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelEvent.eventData.collectLatest{
                    adapter.submitData(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collectLatest { state ->
                    if (state.refresh.endOfPaginationReached) {
                        binding.recyclerList.scrollToPosition (0)
                    }
                    binding.swiperefresh.isRefreshing = state.refresh is LoadState.Loading
                }
            }
        }

        viewModelEvent.dataState.observe(viewLifecycleOwner) { feedModelState ->
            binding.progress.isVisible = feedModelState.loading
            binding.errorGroup.isVisible = feedModelState.error
        }

        binding.retryButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModelEvent.eventData.collectLatest {
                    adapter.submitData(it)
                }
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            adapter.refresh()
        }

        binding.addEvent.setOnClickListener {
            if (viewModelAuth.authenticated) {
                findNavController().navigate( R.id.action_allEventsFragment_to_newEventFragment)

            } else {
                signInDialog()
            }

        }


        return binding.root
    }

    private fun signInDialog() {
        val listener = DialogInterface.OnClickListener{ _, which ->
            when(which) {
                DialogInterface.BUTTON_POSITIVE -> findNavController().navigate(R.id.authSignInFragment)
            }
        }
        val dialog = AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(R.string.not_authorized)
            .setMessage(R.string.sign_in_account)
            .setPositiveButton(R.string.yes, listener)
            .setNegativeButton(R.string.no, listener)
            .create()
        dialog.show()
    }

    private fun signOutDialog() {
        val listener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    appAuth.removeAuth()
                    findNavController().navigate(R.id.mainFragment)
                }
            }
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setTitle(R.string.are_you_sure)
            .setPositiveButton(R.string.yes, listener)
            .setNegativeButton(R.string.no, listener)
            .create()
        dialog.show()
    }
}