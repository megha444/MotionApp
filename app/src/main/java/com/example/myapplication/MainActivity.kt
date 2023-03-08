package com.example.myapplication
import Constants.ACTIVITY_TRANSITION_STORAGE
import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.material.switchmaterial.SwitchMaterial
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    lateinit var client: ActivityRecognitionClient
    lateinit var storage: SharedPreferences
    private lateinit var switchActivityTransition: SwitchMaterial
    private lateinit var tvMainActivity: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // The Activity Recognition Client returns a
        // list of activities that a user might be doing
        client = ActivityRecognition.getClient(this)

        // variable to check whether the user have already given the permissions
        storage = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)

        switchActivityTransition = findViewById(R.id.switchActivityTransition)
        tvMainActivity = findViewById(R.id.tvMainActivity)

        val sdf = SimpleDateFormat("dd MMM yyyy hh:mm a")
        val currentDate = sdf.format(Date())
        tvMainActivity.setText("Today:\n"+currentDate)

        // check switch is on/off
        switchActivityTransition.isChecked = getSwitchState()

        switchActivityTransition.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // check for devices with Android 10 (29+).
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    // check for permission
                    && !ActivityTransitionsUtil.hasActivityTransitionPermissions(this)
                ) {
                    switchActivityTransition.isChecked = false
                    // request for permission
                    requestActivityTransitionPermission()
                } else {
                    // when permission is already allowed
                    requestForUpdates()
                }
            } else {
                saveSwitchState(false)
                deregisterForUpdates()
            }
        }
    }

    // when permission is denied
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        // permission is denied permanently
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestActivityTransitionPermission()
        }
    }

    // after giving permission
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        switchActivityTransition.isChecked = true
        saveSwitchState(true)
        requestForUpdates()
    }

    // request for permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    // To register for changes we have to also supply the requestActivityTransitionUpdates() method
    // with the PendingIntent object that will contain an intent to the component
    // (i.e. IntentService, BroadcastReceiver etc.) that will receive and handle updates appropriately.
    private fun requestForUpdates() {
        client
            .requestActivityTransitionUpdates(
                ActivityTransitionsUtil.getActivityTransitionRequest(),
                getPendingIntent()
            )
            .addOnSuccessListener {
                showToast("successful registration")
            }
            .addOnFailureListener {
                showToast("Unsuccessful registration")
            }
    }

    // Deregistering from updates
    // call the removeActivityTransitionUpdates() method
    // of the ActivityRecognitionClient and pass
    // ourPendingIntent object as a parameter
    private fun deregisterForUpdates() {
        client
            .removeActivityTransitionUpdates(getPendingIntent())
            .addOnSuccessListener {
                getPendingIntent().cancel()
                showToast("successful deregistration")
            }
            .addOnFailureListener { e: Exception ->
                showToast("unsuccessful deregistration")
            }
    }

    // creates and returns the PendingIntent object which holds
    // an Intent to an BroadCastReceiver class
    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            Constants.REQUEST_CODE_INTENT_ACTIVITY_TRANSITION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // requesting for permission
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestActivityTransitionPermission() {
        EasyPermissions.requestPermissions(
            this,
            "You need to allow Activity Transition Permissions in order to recognize your activities",
            Constants.REQUEST_CODE_ACTIVITY_TRANSITION,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG)
            .show()
    }

    // save switch state
    private fun saveSwitchState(value: Boolean) {
        storage
            .edit()
            .putBoolean(ACTIVITY_TRANSITION_STORAGE, value)
            .apply()
    }

    // get the state of switch
    private fun getSwitchState() = storage.getBoolean(ACTIVITY_TRANSITION_STORAGE, false)
}
