package ru.netology.diploma.adapter

import android.net.Uri
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.netology.diploma.R
import ru.netology.diploma.databinding.FragmentOneEventBinding
import ru.netology.diploma.dto.AttachmentType
import ru.netology.diploma.dto.Event
import ru.netology.diploma.util.formatDateTime
import ru.netology.diploma.util.load
import ru.netology.diploma.util.loadCircle
import ru.netology.diploma.util.numberRepresentation

interface OnInteractionListenerEvent {
    fun like(event: Event)
    fun remove(event: Event)
    fun edit(event: Event)
    fun showEvent(event: Event)
    fun playMusic (event: Event)
}

class EventAdapter (private val onInteractionListener: OnInteractionListenerEvent) :
    PagingDataAdapter<Event, EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding =
            FragmentOneEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        event?.let { holder.bind(event) }
    }
}

class EventViewHolder(
    private val binding: FragmentOneEventBinding,
    private val onInteractionListener: OnInteractionListenerEvent
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(event: Event) {
        binding.apply {
            authorEvent.text = event.author
            eventPublished.text = formatDateTime(event.published)
            dateEvent.text = event.datetime?.let { formatDateTime(it) }
            if (event.type != null) { format.text = event.type.toString() } else format.text = ""
            content.text = event.content
            content.movementMethod = ScrollingMovementMethod()
            likesIcon.isChecked = event.likedByMe
            likesIcon.text = numberRepresentation (event.likeOwnerIds.size)
            attachmentImage.visibility = View.GONE
            music.visibility = View.GONE

            if (event.link != null) {
                videoLink.visibility = View.VISIBLE
            } else videoLink.visibility = View.GONE


            val urlAvatar = "${event.authorAvatar}"
            avatarEvent.loadCircle(urlAvatar)

            if (event.attachment?.url != null) {
                when (event.attachment.type) {
                    AttachmentType.AUDIO -> {
                        music.visibility = View.VISIBLE
                        if (event.attachment.isPlaying) {
                            playButton.setIconResource(R.drawable.ic_pause_24)
                            playButton.setText(R.string.stop_audio)
                        } else {
                            playButton.setIconResource(R.drawable.ic_play_24)
                            playButton.setText(R.string.play_audio)
                        }

                        playButton.setOnClickListener {
                            onInteractionListener.playMusic(event)
                        }
                    }

                    AttachmentType.IMAGE -> {
                        event.attachment.url.let {
                            val url = event.attachment.url
                            attachmentImage.load(url)
                        }
                        attachmentImage.visibility = View.VISIBLE
                    }


                    AttachmentType.VIDEO -> {

                        videoLink.visibility = View.VISIBLE

                        val uri = Uri.parse(event.attachment.url)
                        videoLink.setVideoURI(uri)
                        videoLink.setOnPreparedListener { mediaPlayer ->
                            mediaPlayer?.setVolume(0F, 0F)
                            mediaPlayer?.isLooping = true
                            videoLink.start()
                        }
                    }

                    else -> Unit
                }
            }


            likesIcon.setOnClickListener {
                onInteractionListener.like(event)
            }

            menuOnePost.isVisible = event.ownedByMe

            menuOnePost.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.post_options_menu)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.remove -> {
                                onInteractionListener.remove(event)
                                true
                            }

                            R.id.edit -> {
                                onInteractionListener.edit(event)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            content.setOnClickListener {
                onInteractionListener.showEvent(event)
            }

            format.setOnClickListener {
                onInteractionListener.showEvent(event)
            }
            dateEvent.setOnClickListener {
                onInteractionListener.showEvent(event)
            }
            attachmentImage.setOnClickListener {
                onInteractionListener.showEvent(event)
            }

        }
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem == newItem
    }
}