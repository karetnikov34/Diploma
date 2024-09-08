package ru.netology.diploma.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.diploma.R
import ru.netology.diploma.adapter.JobAdapter
import ru.netology.diploma.adapter.OnInteractionListenerJob
import ru.netology.diploma.adapter.OnInteractionListenerWall
import ru.netology.diploma.adapter.WallAdapter
import ru.netology.diploma.databinding.FragmentSingleUserCardBinding
import ru.netology.diploma.dto.Job
import ru.netology.diploma.dto.Post
import ru.netology.diploma.dto.UserResponse
import ru.netology.diploma.util.JobDealtWith
import ru.netology.diploma.util.UserDealtWith
import ru.netology.diploma.util.load
import ru.netology.diploma.viewmodel.AuthViewModel
import ru.netology.diploma.viewmodel.PostViewModel
import ru.netology.diploma.viewmodel.UserViewModel

@AndroidEntryPoint
class OneUserCardFragment : Fragment() {

    private val viewModelPost: PostViewModel by activityViewModels()
    private val viewModelAuth: AuthViewModel by activityViewModels()
    private val viewModelUser: UserViewModel by activityViewModels()

    private val mediaObserver = MediaLifecycleObserver()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSingleUserCardBinding.inflate(
            inflater,
            container,
            false
        )

        val user = UserDealtWith.get()

        val adapterWall = WallAdapter(object : OnInteractionListenerWall {
            override fun like(post: Post) {
                if (viewModelAuth.authenticated) {
                    viewModelPost.likeById(post)
                } else {
                    signInDialog()
                }
            }

            var trackId = -1
            override fun playMusic(post: Post) {
                val thisTrackId = post.id
                if (mediaObserver.player?.isPlaying == true) {
                    mediaObserver.apply {
                        viewModelPost.updateIsPlayingWall(post.id, false)
                        viewModelPost.updatePlayer()
                        stop()
                        if (thisTrackId != trackId) {
                            trackId = thisTrackId
                            viewModelPost.updateIsPlayingWall(post.id, true)
                            post.attachment?.url?.let { play(it) }
                        }
                    }
                } else {
                    mediaObserver.apply {
                        trackId = thisTrackId
                        viewModelPost.updateIsPlayingWall(post.id, true)
                        post.attachment?.url?.let { play(it) }
                    }
                }
            }

        })

        mediaObserver.player?.setOnCompletionListener {
            mediaObserver.player?.stop()
            viewModelPost.updatePlayer()
        }

        val adapterJob = JobAdapter(object : OnInteractionListenerJob {
            override fun editJob(job: Job) {
                JobDealtWith.saveJobDealtWith(job)
                findNavController().navigate(R.id.action_oneUserCardFragment_to_editJobFragment)
            }

            override fun removeJob(job: Job) {
                viewModelUser.removeJobByd(job.id)
            }
        })


        val tabLayout: TabLayout = binding.tabLayout
        tabLayout.addTab(tabLayout.newTab().setText(R.string.Wall), 0, true)
        tabLayout.addTab(tabLayout.newTab().setText(R.string.Jobs), 1)



        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        viewModelUser.loadWall(user.id)
                        binding.recyclerList.adapter = adapterWall
                    }

                    1 -> {
                        if (user.id == viewModelAuth.authenticatedId) {
                            viewModelUser.loadMyJobs()
                        } else {
                            viewModelUser.loadJobs(user.id)
                        }
                        binding.recyclerList.adapter = adapterJob
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Проверка выбранной вкладки при инициализации
        val selectedTabPosition = tabLayout.selectedTabPosition
        val selectedTabText = tabLayout.getTabAt(selectedTabPosition)?.text.toString()

        if (selectedTabText == "Wall") {
            viewModelUser.loadWall(UserDealtWith.get().id)
            binding.recyclerList.adapter = adapterWall
        } else {
            if (user.id == viewModelAuth.authenticatedId) {
                viewModelUser.loadMyJobs()
            } else {
                viewModelUser.loadJobs(user.id)
            }
            binding.recyclerList.adapter = adapterJob
        }

        fun bind(user: UserResponse) {
            binding.apply {
                nameBar.text = user.name

                addPostJob.isVisible = (user.id == viewModelAuth.authenticatedId)

                photoUser.isVisible = (user.avatar != null)
                if (user.avatar == null) {
                    photoUser.visibility = View.GONE
                }
                val photo = "${user.avatar}"
                photoUser.load(photo)

            }
        }

        bind(user)

        binding.back.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.addPostJob.setOnClickListener {

            PopupMenu(it.context, it).apply {
                inflate(R.menu.post_job_menu)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.addPost -> {
                            findNavController().navigate(R.id.action_oneUserCardFragment_to_newPostFragment)
                            true
                        }

                        R.id.addJob -> {
                            findNavController().navigate(R.id.action_oneUserCardFragment_to_newJobFragment)
                            true
                        }

                        else -> false
                    }
                }
            }.show()

        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelUser.wall.collectLatest {
                    adapterWall.submitList(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelUser.jobs.collectLatest {
                    adapterJob.submitList(it)
                }
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
}