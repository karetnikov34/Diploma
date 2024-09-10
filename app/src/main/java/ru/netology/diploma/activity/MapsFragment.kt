package ru.netology.diploma.activity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.GeoObjectTapEvent
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.diploma.R
import ru.netology.diploma.databinding.FragmentMapBinding
import ru.netology.diploma.viewmodel.PostViewModel

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MapsFragment : Fragment(), GeoObjectTapListener {

    private val viewModelPost: PostViewModel by activityViewModels()

    private var mapView: MapView? = null
    private lateinit var userLocation: UserLocationLayer

    private val locationObjectListener = object : UserLocationObjectListener {
        override fun onObjectAdded(view: UserLocationView) = Unit

        override fun onObjectRemoved(view: UserLocationView) = Unit

        override fun onObjectUpdated(view: UserLocationView, event: ObjectEvent) {
            userLocation.cameraPosition()?.target?.let {
                mapView?.mapWindow?.map?.move(CameraPosition(it, 17F, 0F, 0F))
            }
            userLocation.setObjectListener(null)
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            when {
                granted -> {
                    userLocation.isVisible = true
                    userLocation.isHeadingEnabled = false
                    userLocation.cameraPosition()?.target?.also {
                        val map = mapView?.mapWindow?.map ?: return@registerForActivityResult
                        val cameraPosition = map.cameraPosition
                        map.move(
                            CameraPosition(
                                it,
                                17F,
                                //cameraPosition.zoom,
                                cameraPosition.azimuth,
                                cameraPosition.tilt,
                            )
                        )
                    }
                }

                else -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.need_permission),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentMapBinding.inflate(inflater, container, false)

        lateinit var userLocationPoint: Point
        val defaultPosition = Point(55.752004, 37.617734)


        MapKitFactory.initialize(requireContext())
        mapView = binding.mapview
        val map = mapView?.mapWindow?.map!!

        binding.mapview.apply {
            userLocation = MapKitFactory.getInstance().createUserLocationLayer(mapWindow)
            if (requireActivity()
                    .checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
            ) {
                userLocation.isVisible = true
                userLocation.isHeadingEnabled = false
                userLocationPoint = userLocation.cameraPosition()?.target ?: defaultPosition
            }
        }

        userLocation.setObjectListener(locationObjectListener)

        map.apply {
            isScrollGesturesEnabled = true
            isZoomGesturesEnabled = true
        }

        val toast = Toast.makeText(context, R.string.tap_the_place_to_choose, Toast.LENGTH_LONG)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()

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

        binding.myLocation.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        map.addTapListener(this)

        binding.choosePlaceButton.visibility = View.GONE

        viewModelPost.placeName.observe(viewLifecycleOwner) {
            binding.choosePlaceButton.isVisible = (it != null)
            val textToDisplay = context?.getString(R.string.tap_to_save_location) + " " + it
            binding.choosePlaceButton.text = textToDisplay
        }

        binding.choosePlaceButton.setOnClickListener {
            binding.choosePlaceButton.visibility = View.GONE
            viewModelPost.placeName.value = null
            findNavController().navigateUp()
        }

        val imageProvider =
            ImageProvider.fromResource(requireContext(), R.drawable.ic_map_marker_icon)

        viewModelPost.coords.observe(viewLifecycleOwner) { coords ->

            map.mapObjects.clear()
            coords ?: return@observe

            if (coords.lat != null && coords.long != null) {
                map.mapObjects.addPlacemark().apply {
                    geometry = Point(coords.lat, coords.long)
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
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        mapView?.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onObjectTap(geoObjectTapEvent: GeoObjectTapEvent): Boolean {
        val objectGeometry = geoObjectTapEvent.geoObject.geometry.component1().point
        if (objectGeometry != null) {
            geoObjectTapEvent.geoObject.name?.let { name ->
                viewModelPost.setCoords(objectGeometry.latitude, objectGeometry.longitude)
                viewModelPost.placeName.value = name
            }
        }
        return true
    }
}