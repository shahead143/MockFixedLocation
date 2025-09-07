package com.talha.mockfixedlocation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {
    private lateinit var fused: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private var mockEnabled = false

    // Your exact coords:
    private val FIXED_LAT = 34.83042280501962
    private val FIXED_LON = 71.8340051037514

    private val locationPermRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fused = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            locationPermRequester.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        findViewById<TextView>(R.id.status).text =
            "Fixed to $FIXED_LAT, $FIXED_LON"

        findViewById<Button>(R.id.setBtn).setOnClickListener { startMocking(FIXED_LAT, FIXED_LON) }
        findViewById<Button>(R.id.stopBtn).setOnClickListener { stopMocking() }
    }

    private fun startMocking(lat: Double, lon: Double) {
        fused.setMockMode(true).addOnSuccessListener {
            mockEnabled = true
            Toast.makeText(this, "Mock mode ON", Toast.LENGTH_SHORT).show()

            handler.post(object : Runnable {
                override fun run() {
                    if (!mockEnabled) return
                    val loc = Location("fused")
                    loc.latitude = lat
                    loc.longitude = lon
                    loc.accuracy = 1.0f
                    loc.time = System.currentTimeMillis()
                    if (Build.VERSION.SDK_INT >= 17) {
                        loc.elapsedRealtimeNanos = System.nanoTime()
                    }
                    fused.setMockLocation(loc).addOnFailureListener {
                        Toast.makeText(this@MainActivity, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                    handler.postDelayed(this, 2000) // keep feeding every 2s
                }
            })
        }.addOnFailureListener {
            Toast.makeText(this, "Select this app as Mock location app in Developer options", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopMocking() {
        mockEnabled = false
        fused.setMockMode(false).addOnCompleteListener {
            Toast.makeText(this, "Mock mode OFF", Toast.LENGTH_SHORT).show()
        }
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMocking()
    }
}
