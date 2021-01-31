package com.example.kooootlin.services

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kooootlin.R
import com.example.kooootlin.model.PermissionFeature
import java.util.*


class PermissionService : AppCompatActivity() {
    private var allPermissionAskedGranted = false
    private var feature: PermissionFeature? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra("INTENT_EXTRA_FEATURE_PERMISSION")) {
            feature =
                intent.getSerializableExtra("INTENT_EXTRA_FEATURE_PERMISSION") as PermissionFeature?
            if (feature == null || feature!!.permissions == null || feature!!.permissions?.size!! < 1) {
                setResult(Activity.RESULT_CANCELED, null)
                finish()
            }
            checkPermissions()
        }
    }

    private fun shareCheckPermission() {
        // Store allPermissionGranted boolean for loginActivity
        val editor = getSharedPreferences(
            "CODE_PERMISSION",
            Context.MODE_PRIVATE
        ).edit()
        editor.putBoolean(feature?.featureKey, allPermissionAskedGranted)
        editor.apply()
        val resultCode =
            if (allPermissionAskedGranted) Activity.RESULT_OK else Activity.RESULT_CANCELED
        setResult(resultCode)
        finish()
    }

    private fun checkPermissions() {
        try {
            // Check which permission is granted
            val listPermissionNeeded: MutableList<String> =
                ArrayList()
            for (perm in feature?.permissions!!) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        perm
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    listPermissionNeeded.add(perm)
                }
            }

            // Ask for non-granted permissions
            if (!listPermissionNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(
                    this, listPermissionNeeded.toTypedArray(),
                    998
                )
            } else {
                allPermissionAskedGranted = true
                shareCheckPermission()
            }
        } catch (e: Exception) {
            Log.e("checkPermissions()", e.toString())
            shareCheckPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != 998) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        val permissionResults =
            HashMap<String, Int>()
        var deniedCount = 0
        for (i in grantResults.indices) {
            // Add only permissions which are denied
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                permissionResults[permissions[i]] = grantResults[i]
                deniedCount++
            }
        }
        if (deniedCount == 0) {
            allPermissionAskedGranted = true
            shareCheckPermission()
        } else {
            permissionIsDenied(permissionResults)
        }
    }

    private fun permissionIsDenied(permissionResults: HashMap<String, Int>) {
        var permNeverAskIsChecked = false
        for ((permName) in permissionResults) {
            // Permission is denied "never ask again" is not checked
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permName)) {
                permNeverAskIsChecked = true
                break
            }
        }
        if (permNeverAskIsChecked) {
            permNeverAskIsChecked()
        } else {
            permNeverAskIsNotChecked()
        }
    }

    private fun permNeverAskIsNotChecked() {
        showDialog(
            "", feature?.messageAskPermission,
            getString(R.string.acceptPermission),
            DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
                checkPermissions()
            },
            getString(R.string.noAcceptPermission),
            DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
                shareCheckPermission()
            }, false
        )
    }

    private fun permNeverAskIsChecked() {
        showDialog(
            "",
           "Vous n\'avez oas accordé certaines autorisations. Accordé les autorisations dans [Paramètre] > [Autorisations]",
            getString(R.string.acceptPermission),
            DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                shareCheckPermission()
            },
            getString(R.string.noAcceptPermission),
            DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
                shareCheckPermission()
            }, false
        )
    }

    fun showDialog(
        title: String?, msg: String?, positiveLabel: String?,
        positiveOnClick: DialogInterface.OnClickListener?,
        negativeLabel: String?, negativeOnClick: DialogInterface.OnClickListener?,
        isCancelAble: Boolean
    ): AlertDialog {
        val builder =
            AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setCancelable(isCancelAble)
        builder.setMessage(msg)
        builder.setPositiveButton(positiveLabel, positiveOnClick)
        builder.setNegativeButton(negativeLabel, negativeOnClick)
        val alert = builder.create()
        alert.show()
        return alert
    }
}
