package com.example.speedtracker  // made with help from claude <3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

/**
 * SpeedTracker - GPS Speed Monitoring App
 * Made with help from Claude <3
 * 
 * This app tracks your speed using GPS and allows sharing during active sessions.
 * Features: Real-time speed display, session management, and speed sharing.
 */
class MainActivity : AppCompatActivity() {

    // Location services components
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    
    // App state
    private var currentSpeed: Float = 0f  // Current speed in km/h
    private var isSessionActive: Boolean = false  // Track session state
    private val locationPermissionCode = 1001
    
    // UI components
    private lateinit var speedTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var shareButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        initializeViews()
        
        // Initialize location services
        initializeLocationServices()
        
        // Set up button click listeners
        setupButtonListeners()
        
        // Update initial button states
        updateButtonStates()
    }

    /**
     * Initialize all UI view references
     */
    private fun initializeViews() {
        speedTextView = findViewById(R.id.speedTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        shareButton = findViewById(R.id.shareButton)
    }

    /**
     * Configure location services and callbacks
     */
    private fun initializeLocationServices() {
        // Get the fused location provider client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configure location request for high accuracy GPS updates
        // Request updates every 2 seconds for responsive speed tracking
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,  // Use GPS for best accuracy
            2000  // Update interval in milliseconds
        ).apply {
            setMinUpdateIntervalMillis(1000)  // Fastest update rate
            setWaitForAccurateLocation(false)  // Don't wait for perfect accuracy
        }.build()

        // Define callback for receiving location updates
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                processLocationUpdate(locationResult)
            }
        }
    }

    /**
     * Process new location data and extract speed
     */
    private fun processLocationUpdate(locationResult: LocationResult) {
        for (location in locationResult.locations) {
            if (location.hasSpeed()) {
                // Convert m/s to km/h (multiply by 3.6)
                currentSpeed = location.speed * 3.6f
                updateSpeedDisplay()
            } else {
                // GPS doesn't have speed data yet
                speedTextView.text = "Speed: Acquiring GPS..."
            }
        }
    }

    /**
     * Update the speed display with current value
     */
    private fun updateSpeedDisplay() {
        val displaySpeed = if (currentSpeed < 1) 0 else currentSpeed.toInt()
        speedTextView.text = "Speed: $displaySpeed km/h"
    }

    /**
     * Configure all button click handlers
     */
    private fun setupButtonListeners() {
        // Start session button
        startButton.setOnClickListener {
            if (checkLocationPermission()) {
                startSpeedTracking()
            } else {
                requestLocationPermission()
            }
        }

        // Stop session button
        stopButton.setOnClickListener {
            stopSpeedTracking()
        }

        // Share speed button
        shareButton.setOnClickListener {
            if (isSessionActive && currentSpeed > 0) {
                shareCurrentSpeed()
            } else if (!isSessionActive) {
                Toast.makeText(this, "Please start a session first", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Waiting for GPS signal...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Start GPS tracking session
     */
    private fun startSpeedTracking() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            isSessionActive = true
            updateButtonStates()
            Toast.makeText(this, "Session started - Drive safely!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Stop GPS tracking session
     */
    private fun stopSpeedTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isSessionActive = false
        currentSpeed = 0f
        speedTextView.text = "Speed: 0 km/h"
        updateButtonStates()
        Toast.makeText(this, "Session stopped", Toast.LENGTH_SHORT).show()
    }

    /**
     * Share current speed via system share dialog
     */
    private fun shareCurrentSpeed() {
        val speedKmh = currentSpeed.toInt()
        val speedMph = (currentSpeed * 0.621371).toInt()
        
        val shareText = buildString {
            append("🚗 Current Speed Update 🚗\n")
            append("Speed: $speedKmh km/h ($speedMph mph)\n")
            append("Tracked via SpeedTracker GPS App")
        }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share your speed via"))
    }

    /**
     * Update button enabled states based on session
     */
    private fun updateButtonStates() {
        startButton.isEnabled = !isSessionActive
        stopButton.isEnabled = isSessionActive
        shareButton.isEnabled = isSessionActive
    }

    /**
     * Check if location permission is granted
     */
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request location permission from user
     */
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionCode
        )
    }

    /**
     * Handle permission request result
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start tracking
                startSpeedTracking()
            } else {
                // Permission denied
                Toast.makeText(
                    this,
                    "Location permission required for GPS speed tracking",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Clean up when app is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        if (isSessionActive) {
            stopSpeedTracking()
        }
    }
}