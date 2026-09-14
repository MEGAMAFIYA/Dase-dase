package com.dase.assistant

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createInterface()
    }

    private fun createInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(18, 18, 18))
        }

        val title = TextView(this).apply {
            text = "DASE"
            textSize = 40f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Shaxsiy AI yordamchi"
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "Holat: Boshlang‘ich versiya"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }

        val microphoneButton = Button(this).apply {
            text = "Mikrofon ruxsatini berish"
            setOnClickListener {
                requestMicrophonePermission()
            }
        }

        val info = TextView(this).apply {
            text = "Keyingi bosqichlarda ovozli buyruqlar, AI va Telegram boshqaruvi qo‘shiladi."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 0)
        }

        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            subtitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            statusText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            microphoneButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            info,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)
    }

    private fun requestMicrophonePermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            statusText.text = "Holat: Mikrofon ruxsati berilgan"
            Toast.makeText(
                this,
                "Mikrofon ruxsati mavjud",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MICROPHONE_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == MICROPHONE_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

            statusText.text = if (granted) {
                "Holat: Mikrofon ruxsati berildi"
            } else {
                "Holat: Mikrofon ruxsati berilmadi"
            }
        }
    }

    companion object {
        private const val MICROPHONE_REQUEST_CODE = 1001
    }
}
