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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.diploma.R
import ru.netology.diploma.auth.AppAuth
import ru.netology.diploma.databinding.FragmentMainBinding
import ru.netology.diploma.util.UserDealtWith
import ru.netology.diploma.viewmodel.AuthViewModel
import ru.netology.diploma.viewmodel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {

    private val viewModelAuth: AuthViewModel by activityViewModels()
    private val viewModelPost: PostViewModel by activityViewModels()

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMainBinding.inflate(
            inflater,
            container,
            false
        )

        val toolbar: Toolbar = binding.toolbarMain

        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModelAuth.dataAuth.collect {
                    toolbar.invalidateMenu()
                }
            }
        }

        toolbar.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.setGroupVisible(R.id.unauthenticated, !viewModelAuth.authenticated)
                menu.setGroupVisible(R.id.authenticated, viewModelAuth.authenticated)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.signin -> {
                        findNavController().navigate(R.id.action_mainFragment_to_authSignInFragment)
                        true
                    }

                    R.id.signup -> {
                        findNavController().navigate(R.id.action_mainFragment_to_authSignUpFragment)
                        true
                    }

                    R.id.profile -> {
                        val id = if (viewModelAuth.authenticated) { viewModelAuth.authenticatedId } else 0
                        if (id != 0) {viewModelPost.getUserById(id)}
                        val user = viewModelPost.userList.value
                        if (user != null) {
                            UserDealtWith.saveUserDealtWith(user)
                            findNavController().navigate(R.id.action_mainFragment_to_oneUserCardFragment)

                        }
                        true
                    }

                    R.id.signout -> {
                        signOutDialog()
                        true
                    }

                    else -> false
                }
            }
        })

        val bottomNavigationView = binding.bottomNavigation

        bottomNavigationView.setOnItemSelectedListener {item ->
            when(item.itemId) {
                R.id.posts -> {
                    findNavController().navigate(R.id.action_mainFragment_to_allPostsFragment)
                    true
                }
                R.id.events -> {
                    findNavController().navigate(R.id.action_mainFragment_to_allEventsFragment)
                    true
                }
                R.id.users -> {
                    findNavController().navigate(R.id.action_mainFragment_to_allUsersFragment)
                    true
                }
                else -> false
            }
        }

        return binding.root
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