package ru.netology.diploma.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.diploma.R
import ru.netology.diploma.databinding.FragmentSingleEventBinding
import ru.netology.diploma.dto.AttachmentType
import ru.netology.diploma.dto.Event
import ru.netology.diploma.util.EventDealtWith
import ru.netology.diploma.util.formatDateTime
import ru.netology.diploma.util.load
import ru.netology.diploma.util.loadCircle
import ru.netology.diploma.util.numberRepresentation
import ru.netology.diploma.viewmodel.AuthViewModel
import ru.netology.diploma.viewmodel.EventViewModel

@AndroidEntryPoint
class OneEventFragment : Fragment() {

    private val viewModel: EventViewModel by activityViewModels()
    private val viewModelAuth: AuthViewModel by activityViewModels()
    private val mediaObserver = MediaLifecycleObserver()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSingleEventBinding.inflate(
            inflater,
            container,
            false
        )

        val event = EventDealtWith.get()

        binding.content.movementMethod = ScrollingMovementMethod()

        fun bind(event: Event) {
            binding.apply {
                authorEvent.text = event.author
                publishedEvent.text = formatDateTime(event.published)
                dateEvent.text = event.datetime?.let { formatDateTime(it) }
                if (event.type != null) { format.text = event.type.toString() } else format.text = ""
                placeOfWork.text = event.authorJob ?: context?.getString(R.string.looking_for_a_job)
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
                                playButton.setImageResource(R.drawable.ic_pause_24)
                            } else {
                                playButton.setImageResource(R.drawable.ic_play_24)
                            }

                            playButton.setOnClickListener {
                                if (mediaObserver.player?.isPlaying == true) {
                                    mediaObserver.apply {
                                        viewModel.updateIsPlayingEvent(event.id, false)
                                        playButton.setImageResource(R.drawable.ic_play_24)
                                        stop()
                                    }
                                } else {
                                    mediaObserver.apply {
                                        viewModel.updateIsPlayingEvent(event.id, true)
                                        playButton.setImageResource(R.drawable.ic_pause_24)
                                        event.attachment.url.let { play(it) }
                                    }
                                }
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
                    if (viewModelAuth.authenticated) {
                        viewModel.likeEventById(event)
                    } else {
                        signInDialog()
                    }
                }

                menuOnePost.isVisible = event.ownedByMe

                menuOnePost.setOnClickListener {

                    PopupMenu(it.context, it).apply {
                        inflate(R.menu.post_options_menu)
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.remove -> {
                                    viewModel.removeEventById(event.id)
                                    findNavController().navigateUp()
                                    true
                                }

                                R.id.edit -> {
                                    EventDealtWith.saveEventDealtWith(event)
                                    findNavController().navigate(R.id.action_oneEventFragment_to_editEventFragment)
                                    true
                                }
                                else -> false
                            }
                        }
                    }.show()

                }


                speaker1.isVisible = event.speakerIds.isNotEmpty()
                speaker2.isVisible = event.speakerIds.size >= 2
                speaker3.isVisible = event.speakerIds.size >= 3
                speakerMore.isVisible = event.speakerIds.size >= 4

                event.speakerIds.forEachIndexed { index, userId ->
                    val avaUrl = event.users[userId]?.avatar ?: return@forEachIndexed
                    when (index) {
                        0 -> speaker1.loadCircle(avaUrl)
                        1 -> speaker2.loadCircle(avaUrl)
                        2 -> speaker3.loadCircle(avaUrl)
                        else -> Unit
                    }
                }

                avatarLiker1.isVisible = event.likeOwnerIds.isNotEmpty()
                avatarLiker2.isVisible = event.likeOwnerIds.size >= 2
                avatarLiker3.isVisible = event.likeOwnerIds.size >= 3
                avatarLiker4.isVisible = event.likeOwnerIds.size >= 4
                avatarLiker5.isVisible = event.likeOwnerIds.size >= 5
                avatarLikerMore.isVisible = event.likeOwnerIds.size >= 6

                event.likeOwnerIds.forEachIndexed { index, userId ->
                    val avaUrl = event.users[userId]?.avatar ?: return@forEachIndexed
                    when (index) {
                        0 -> avatarLiker1.loadCircle(avaUrl)
                        1 -> avatarLiker2.loadCircle(avaUrl)
                        2 -> avatarLiker3.loadCircle(avaUrl)
                        3 -> avatarLiker4.loadCircle(avaUrl)
                        4 -> avatarLiker5.loadCircle(avaUrl)
                        else -> Unit
                    }
                }

                avatarParticipant1.isVisible = event.participantsIds.isNotEmpty()
                avatarParticipant2.isVisible = event.participantsIds.size >= 2
                avatarParticipant3.isVisible = event.participantsIds.size >= 3
                avatarParticipant4.isVisible = event.participantsIds.size >= 4
                avatarParticipant5.isVisible = event.participantsIds.size >= 5
                avatarParticipantMore.isVisible = event.participantsIds.size >= 6

                event.participantsIds.forEachIndexed { index, userId ->
                    val avaUrl = event.users[userId]?.avatar ?: return@forEachIndexed
                    when (index) {
                        0 -> avatarParticipant1.loadCircle(avaUrl)
                        1 -> avatarParticipant2.loadCircle(avaUrl)
                        2 -> avatarParticipant3.loadCircle(avaUrl)
                        3 -> avatarParticipant4.loadCircle(avaUrl)
                        4 -> avatarParticipant5.loadCircle(avaUrl)
                        else -> Unit
                    }
                }

                MapKitFactory.initialize(requireContext())
                val mapView = binding.mapview
                val map = mapView.mapWindow.map

                binding.mapGroup.isVisible = (event.coords?.lat != null && event.coords.long != null)
                if (event.coords?.lat != null && event.coords.long != null) {
                    map.move(
                        CameraPosition(
                            Point(
                                event.coords.lat,
                                event.coords.long
                            ),
                            /* zoom = */ 13.0f,
                            /* azimuth = */ 150.0f,
                            /* tilt = */ 30.0f
                        )
                    )

                }


                val imageProvider =
                    ImageProvider.fromResource(requireContext(), R.drawable.ic_map_marker_icon)
                map.mapObjects.addPlacemark().apply {
                    if (event.coords?.lat != null && event.coords.long != null) {
                        geometry = Point(event.coords.lat, event.coords.long)
                        setIcon(imageProvider)
                        setIconStyle(
                            IconStyle().apply {
                                anchor = PointF(0.5f, 1.0f)
                                scale = 0.08f
                                zIndex = 10F
                            }
                        )
                    }

                }


                binding.zoomInButton.setOnClickListener {
                    val cameraPosition = map.cameraPosition
                    val newZoom = cameraPosition.zoom + 1.0f
                    val newCameraPosition = CameraPosition(
                        cameraPosition.target,
                        newZoom,
                        cameraPosition.azimuth,
                        cameraPosition.tilt
                    )
                    map.move(newCameraPosition)
                }

                binding.zoomOutButton.setOnClickListener {
                    val cameraPosition = map.cameraPosition
                    val newZoom = cameraPosition.zoom - 1.0f
                    val newCameraPosition = CameraPosition(
                        cameraPosition.target,
                        newZoom,
                        cameraPosition.azimuth,
                        cameraPosition.tilt
                    )
                    map.move(newCameraPosition)
                }


            }
        }

        bind(event)

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