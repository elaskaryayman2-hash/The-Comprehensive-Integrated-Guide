package com.example.medicalfinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.os.LocaleListCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedDistanceKm = 5
    private var currentLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // تهيئة الخريطة
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 1. تشغيل أزرار الطوارئ (اتصال مباشر)
        findViewById<Button>(R.id.btnAmbulance).setOnClickListener { dialEmergency("123") }
        findViewById<Button>(R.id.btnPolice).setOnClickListener { dialEmergency("122") }
        findViewById<Button>(R.id.btnFire).setOnClickListener { dialEmergency("180") }

        // 2. زر تبديل اللغة (عربي / إنجليزي)
        findViewById<Button>(R.id.btnLang).setOnClickListener {
            val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            val nextLang = if (currentLang.contains("ar")) "en" else "ar"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLang))
        }

        // 3. شريط تحديد المسافة من 1 إلى 20 كم
        val tvDistance = findViewById<TextView>(R.id.tvDistance)
        val seekBar = findViewById<SeekBar>(R.id.seekBarDistance)
        tvDistance.text = getString(R.string.distance_label, selectedDistanceKm)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedDistanceKm = progress + 1 // لضمان أن النطاق من 1 إلى 20
                tvDistance.text = getString(R.string.distance_label, selectedDistanceKm)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 4. تشغيل أزرار البحث (طبيب - مستشفى - صيدلية)
        val etDoctor = findViewById<EditText>(R.id.etDoctorSearch)
        val etCity = findViewById<EditText>(R.id.etCity)

        findViewById<Button>(R.id.btnSearchDoctor).setOnClickListener {
            val query = etDoctor.text.toString().ifEmpty { "Doctor" }
            val city = etCity.text.toString()
            searchOnGoogleMaps("$query $city")
        }

        findViewById<Button>(R.id.btnHospital).setOnClickListener {
            val city = etCity.text.toString()
            searchOnGoogleMaps("Hospital $city")
        }

        findViewById<Button>(R.id.btnPharmacy).setOnClickListener {
            val city = etCity.text.toString()
            searchOnGoogleMaps("Pharmacy $city")
        }
    }

    // دالة فتح شاشة الاتصال برقم الطوارئ مباشرة
    private fun dialEmergency(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        startActivity(intent)
    }

    // دالة البحث في خرائط جوجل ضمن المسافة المحددة
    private fun searchOnGoogleMaps(searchQuery: String) {
        val location = currentLatLng
        val uriString = if (location != null) {
            "geo:${location.latitude},${location.longitude}?q=${Uri.encode(searchQuery)}&z=13"
        } else {
            "geo:0,0?q=${Uri.encode(searchQuery)}"
        }
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // فتح في المتصفح في حال عدم وجود تطبيق خرائط جوجل
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com{Uri.encode(searchQuery)}"))
            startActivity(webIntent)
        }
    }

    // تحديد موقع المستخدم الحالي عند جاهزية الخريطة
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        mMap.isMyLocationEnabled = true
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 14f))
                mMap.addMarker(MarkerOptions().position(currentLatLng!!).title("موقعك الحالي / Your Location"))
            }
        }
    }
}
