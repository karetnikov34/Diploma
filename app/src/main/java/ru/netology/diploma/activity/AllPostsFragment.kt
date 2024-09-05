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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.diploma.R
import ru.netology.diploma.adapter.OnInteractionListener
import ru.netology.diploma.adapter.PostAdapter
import ru.netology.diploma.auth.AppAuth
import ru.netology.diploma.databinding.FragmentAllPostsBinding
import ru.netology.diploma.dto.Post
import ru.netology.diploma.util.PostDealtWith
import ru.netology.diploma.viewmodel.AuthViewModel
import ru.netology.diploma.viewmodel.PostViewModel
import javax.inject.Inject

class AllPostsFragment : Fragment() {

    private val viewModelPost: PostViewModel by activityViewModels()
    private val viewModelAuth: AuthViewModel by activityViewModels()

    private val mediaObserver = MediaLifecycleObserver()

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAllPostsBinding.inflate(
            inflater,
            container,
            false
        )

        var trackId = -1

        val adapter = PostAdapter(object : OnInteractionListener {
            override fun like(post: Post) {
                if (viewModelAuth.authenticated) {
                    viewModelPost.likeById(post)
                } else {
                    signInDialog()
                }
            }

            override fun remove(post: Post) {
                viewModelPost.removeById(post.id)
            }

            override fun edit(post: Post) {
                PostDealtWith.savePostDealtWith(post)
                findNavController().navigate(R.id.action_allPostsFragment_to_editPostFragment)
            }

            override fun showPost(post: Post) {
                PostDealtWith.savePostDealtWith(post)
                findNavController().navigate(R.id.action_allPostsFragment_to_onePostFragment)
            }

            override fun playMusic(post: Post) {

                val thisTrackId = post.id
                if (mediaObserver.player?.isPlaying == true) {
                    mediaObserver.apply {
                        viewModelPost.updateIsPlaying(post.id, false)
                        viewModelPost.updatePlayer()
                        stop()
                        if (thisTrackId != trackId ) {
                            trackId = thisTrackId
                            viewModelPost.updateIsPlaying(post.id, true)
                            post.attachment?.url?.let { play(it) }
                        }
                    }
                } else {
                    mediaObserver.apply {
                        trackId = thisTrackId
                        viewModelPost.updateIsPlaying(post.id, true)
                        post.attachment?.url?.let { play(it) }
                    }
                }
            }

        }
        )

        mediaObserver.player?.setOnCompletionListener {
            mediaObserver.player?.stop()
            viewModelPost.updatePlayer()
        }

        val toolbar: Toolbar = binding.toolbarPosts

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModelAuth.dataAuth.collect {
                    toolbar.invalidateMenu()
                }
            }
        }

        binding.home.setOnClickListener {
            findNavController().navigate(R.id.mainFragment)
        }

        toolbar.addMenuProvider(object : MenuProvider {
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
                        findNavController().navigate(R.id.action_allPostsFragment_to_authSignInFragment)
                        true
                    }

                    R.id.signup -> {
                        findNavController().navigate(R.id.action_allPostsFragment_to_authSignUpFragment)
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
                viewModelPost.data.collectLatest{
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

        viewModelPost.dataState.observe(viewLifecycleOwner) { feedModelState ->
            binding.progress.isVisible = feedModelState.loading
            binding.errorGroup.isVisible = feedModelState.error
        }

        binding.retryButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModelPost.data.collectLatest {
                    adapter.submitData(it)
                }
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            adapter.refresh()
        }

        binding.addPost.setOnClickListener {
            if (viewModelAuth.authenticated) {
                findNavController().navigate(
                    R.id.action_allPostsFragment_to_newPostFragment,
                )
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