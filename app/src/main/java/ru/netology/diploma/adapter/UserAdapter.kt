package ru.netology.diploma.adapter

import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.diploma.adapter.UserAdapter.Companion.choosing
import ru.netology.diploma.databinding.FragmentOneUserCardBinding
import ru.netology.diploma.dto.UserResponse
import ru.netology.diploma.util.loadCircle

interface OnInteractionListenerUser {
    fun show(user: UserResponse)
    fun choose(user: UserResponse)
}

class UserAdapter(private val onInteractionListener: OnInteractionListenerUser) :
    ListAdapter<UserResponse, UserViewHolder>(UserDiffCallback()) {

    companion object {
        var choosing = false
        val selectedItems = SparseBooleanArray()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding =
            FragmentOneUserCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding, onInteractionListener, this)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    fun toggleSelection(position: Int) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }
}

class UserViewHolder(
    private val binding: FragmentOneUserCardBinding,
    private val onInteractionListener: OnInteractionListenerUser,
    private val adapter: UserAdapter
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(userResponse: UserResponse) {
        binding.apply {

            val urlAvatar = "${userResponse.avatar}"
            avatarUser.loadCircle(urlAvatar)

            userName.text = userResponse.name
            userLogin.text = userResponse.login

            userName.setOnClickListener {
                onInteractionListener.show(userResponse)
            }
            userLogin.setOnClickListener {
                onInteractionListener.show(userResponse)
            }
            avatarUser.setOnClickListener {
                onInteractionListener.show(userResponse)
            }

            checkButton.isVisible = choosing


            checkButton.setOnClickListener {

                adapter.toggleSelection(bindingAdapterPosition)

                onInteractionListener.choose(userResponse)
            }
        }
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<UserResponse>() {
    override fun areItemsTheSame(oldItem: UserResponse, newItem: UserResponse): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: UserResponse, newItem: UserResponse): Boolean {
        return oldItem == newItem
    }
}