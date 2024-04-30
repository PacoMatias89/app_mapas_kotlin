package me.franciscomolina.molina_paco_mapas

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import com.google.android.gms.maps.model.*
import com.google.android.material.navigation.NavigationView
import me.franciscomolina.molina_paco_mapas.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnMyLocationButtonClickListener, OnMyLocationClickListener, NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var map: GoogleMap
    private lateinit var mapManager: MapManager


    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setSupportActionBar(binding.toolbar)
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, findViewById(R.id.toolbar), R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this@MainActivity)
        createFragment()
    }

    private fun createFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.main) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        mapManager = MapManager(this, map)
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMyLocationClickListener(this)
        map.setOnMapClickListener { latLng ->
            mapManager.handleMapClick(latLng)
        }

        map.setOnMapLongClickListener { latLng ->
            mapManager.handleMapLongClick(latLng)
        }


        enableLocation()
        //enableMyLocation()
    }

    private fun enableLocation() {
        if (areLocationPermissionsGranted()) {
            // Verificar si el permiso de ubicación está garantizado antes de habilitar la ubicación en el mapa
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                // Habilitar la ubicación en el mapa
                map.isMyLocationEnabled = true
            }
        } else {
            // Si los permisos de ubicación no están concedidos, solicitar permisos
            requestLocationPermission()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                showInfoDialog("Permiso de ubicación denegado") {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_CODE_LOCATION
                    )
                }
            }
        }
    }



    private fun enableMyLocation() {
        if (areLocationPermissionsGranted()) {
            map.isMyLocationEnabled = true
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            // Centrar el mapa en la ubicación actual
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)) // Ajusta el nivel de zoom según las necesidades
                        } ?: run {
                            showInfoDialog("No se pudo obtener la ubicación actual.") {
                                ActivityCompat.requestPermissions(
                                    this,
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                    REQUEST_CODE_LOCATION
                                )
                            }
                        }
                    }
            }
        } else {
            requestLocationPermission()
        }
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun areLocationPermissionsGranted() = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showInfoDialog("Necesitamos tu permiso para acceder a la ubicación para mejorar la funcionalidad del mapa.") {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.distance -> {
                if (!mapManager.isPolygonClosed) {
                    val numberOfPoints = mapManager.positions.size
                    if (numberOfPoints >= 2) {
                        val distance = mapManager.calculateDistance()
                        showInfoDialog("Distancia entre puntos: ${String.format("%.2f", distance)} metros") {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                REQUEST_CODE_LOCATION
                            )
                        }
                    } else {
                        showInfoDialog("Selecciona al menos dos puntos para calcular la distancia.") {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                REQUEST_CODE_LOCATION
                            )
                        }
                    }
                } else {
                    showInfoDialog("El polígono está cerrado. Por favor, seleccione 'Perímetro' en el menú.") {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_CODE_LOCATION
                        )
                    }
                }
                return true
            }
            R.id.perimeter -> {
                if (!mapManager.isPolygonClosed) {

                    showInfoDialog("El polígono debe estar cerrado. Por favor, seleccione 'Distancia' en el menú.") {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_CODE_LOCATION
                        )
                    }

                }else{
                    if (mapManager.positions.size > 2) {
                        val perimeter = mapManager.calculatePerimeter()
                        showInfoDialog("Perímetro: ${String.format("%.2f", perimeter)} metros") {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                REQUEST_CODE_LOCATION
                            )
                        }
                    } else {
                        showInfoDialog("Selecciona más de dos puntos para calcular el perímetro.") {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                REQUEST_CODE_LOCATION
                            )
                        }
                    }

                }
                return true


            }
            R.id.area -> {
                if (mapManager.positions.size > 2) {
                    val area = mapManager.calculateArea()
                    showInfoDialog("Área: ${String.format("%.2f", area)} metros cuadrados") {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_CODE_LOCATION
                        )
                    }
                } else {
                    showInfoDialog("Selecciona más de dos puntos para calcular el área.") {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_CODE_LOCATION
                        )
                    }
                }
                return true
            }
            R.id.delete -> {
                mapManager.clearMap()
                showInfoDialog("Mapa limpio") {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_CODE_LOCATION
                    )
                }
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    override fun onMyLocationClick(location: Location) {
        showInfoDialog("Ubicación actual:\nLatitud: ${location.latitude}, Longitud: ${location.longitude}") {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION
            )
        }
    }

    private fun showInfoDialog(message: String, function: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}
