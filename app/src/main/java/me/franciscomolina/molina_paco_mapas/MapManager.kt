package me.franciscomolina.molina_paco_mapas
import android.app.AlertDialog
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.SphericalUtil

class MapManager(private val activity: AppCompatActivity, private val map: GoogleMap) {
    var positions = mutableListOf<LatLng>()
    private var polylineOptions = PolylineOptions().width(20f).color(ContextCompat.getColor(activity, R.color.black))
    private var nextMarkerNumber = 1
    private var polyline: Polyline? = null
    var isPolygonClosed = false
    private var isFirstLongClick = true

    fun handleMapClick(latLng: LatLng) {
        if (!isPolygonClosed && !isFirstLongClick) {
            if (positions.isNotEmpty() && isCloseEnough(latLng, positions[0]) && positions.size > 2) {
                closePolygon()
            } else {
                addToPolyline(latLng)
                createMarker(latLng)
            }
        }
    }

    fun handleMapLongClick(latLng: LatLng) {
        // Solo si el polígono no está cerrado y no hay puntos establecidos aún
        if (!isPolygonClosed && positions.isEmpty() && isFirstLongClick) {
            // Establecer la primera marca
            addToPolyline(latLng)
            createMarker(latLng)
            isFirstLongClick = false
        }
    }


    private fun closePolygon() {
        if (positions.size > 2) {
            positions.add(positions[0]) // Añade el primer punto al final para cerrar el polígono
            polylineOptions.add(positions[0])
            polyline = map.addPolyline(polylineOptions)
            polyline?.startCap = RoundCap()
            polyline?.endCap = RoundCap()
            isPolygonClosed = true
            if (validatePolygon(positions)) {
                createPolygon()
                showInfoDialog("Polígono cerrado y validado.")
            } else {
                showInfoDialog("El polígono no es válido. Revisa los puntos.")
            }
        } else {
            showInfoDialog("Se necesitan al menos 3 puntos para cerrar el polígono.")
        }
    }

    private fun validatePolygon(points: List<LatLng>): Boolean {
        // Verifica si el número de puntos es suficiente para formar un polígono.
        if (points.size < 4) return false  // Incluye el punto de cierre, debe tener al menos 4 puntos.

        // Función para calcular la orientación de un trío ordenado de puntos.
        fun orientation(p: LatLng, q: LatLng, r: LatLng): Int {
            val value = (q.longitude - p.longitude) * (r.latitude - q.latitude) -
                    (q.latitude - p.latitude) * (r.longitude - q.longitude)
            return when {
                value > 0 -> 1    // Sentido horario
                value < 0 -> 2    // Sentido antihorario
                else -> 0         // Colineales
            }
        }

        // Función para verificar si dos segmentos se intersectan.
        fun doIntersect(p1: LatLng, q1: LatLng, p2: LatLng, q2: LatLng): Boolean {
            // Encuentra las cuatro orientaciones necesarias para los casos generales.
            val o1 = orientation(p1, q1, p2)
            val o2 = orientation(p1, q1, q2)
            val o3 = orientation(p2, q2, p1)
            val o4 = orientation(p2, q2, q1)

            // Regla general para la intersección de segmentos
            if (o1 != o2 && o3 != o4) return true

            return false
        }

        // Verificar intersecciones entre cada par de segmentos excepto los adyacentes.
        for (i in 0 until points.size - 1) {
            for (j in i + 2 until points.size - 1) {
                if (i == 0 && j == points.size - 2) continue  // Ignora el caso donde el segmento cierra el polígono
                if (doIntersect(points[i], points[i + 1], points[j], points[(j + 1) % points.size])) {
                    return false
                }
            }
        }

        // Si no se encontraron intersecciones, el polígono es válido.
        return true
    }




    private fun isCloseEnough(newPoint: LatLng, startPoint: LatLng): Boolean {
        val result = FloatArray(1)
        Location.distanceBetween(newPoint.latitude, newPoint.longitude, startPoint.latitude, startPoint.longitude, result)
        return result[0] < 10  // clicamos 10 antes de nuestra primera posición para cerrar el polígono
    }

    private fun addToPolyline(coordinate: LatLng) {
        polylineOptions.add(coordinate)
        positions.add(coordinate)
        polyline = map.addPolyline(polylineOptions)
    }

    private fun createMarker(coordinates: LatLng) {
        val markerOptions = MarkerOptions().position(coordinates).title("Punto ${nextMarkerNumber++}")
        val marker = map.addMarker(markerOptions)
        marker?.showInfoWindow()
    }

    private fun createPolygon() {
        val polygonOptions = PolygonOptions()
        polygonOptions.addAll(positions)
        polygonOptions.fillColor(ContextCompat.getColor(activity, R.color.black))
        polygonOptions.strokeColor(ContextCompat.getColor(activity, R.color.black))
        map.addPolygon(polygonOptions)
    }

    private fun showInfoDialog(message: String) {
        activity.runOnUiThread {
            AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
        }
    }

    fun clearMap() {
        map.clear()
        positions.clear()
        polylineOptions = PolylineOptions().width(20f).color(ContextCompat.getColor(activity, R.color.black))
        nextMarkerNumber = 1
        isPolygonClosed = false
        isFirstLongClick = true // Restablecer el estado de isFirstLongClick
    }


    fun calculateDistance(): Double {
        if (positions.size >= 2 && !isPolygonClosed) {
            return SphericalUtil.computeDistanceBetween(positions.first(), positions.last())
        }
        return 0.0
    }

    fun calculatePerimeter(): Double {
        if (positions.size > 2) {
            return SphericalUtil.computeLength(positions)
        }
        return 0.0
    }

    fun calculateArea(): Double {
        if (positions.size >= 2 && isPolygonClosed) {
            return SphericalUtil.computeArea(positions)
        }
        return 0.0
    }
}
