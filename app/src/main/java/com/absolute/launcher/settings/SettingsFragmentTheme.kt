package com.absolute.launcher.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.absolute.launcher.R
import com.absolute.launcher.extern.*
import com.absolute.launcher.intendedSettingsPause
import kotlinx.android.synthetic.main.fragment_settings_theme.*

/** The 'Theme' Tab associated Fragment in Settings */

class SettingsFragmentTheme : Fragment() {

    /** Lifecycle functions */

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings_theme, container, false)
    }

    override fun onStart(){
        // Hide 'select' button for the selected theme or allow customisation
        when (getSavedTheme(context!!)) {
            "dark" -> fragment_settings_theme_select_dark_btn.text="ENABLED"
            "finn" -> fragment_settings_theme_select_finn_btn.text="ENABLED"
            "custom" -> {
                fragment_settings_theme_select_custom_btn.text = getString(R.string.settings_select_image)
                fragment_settings_theme_container.setBackgroundColor(dominantColor)
                setButtonColor(fragment_settings_theme_select_finn_btn, vibrantColor)
                setButtonColor(fragment_settings_theme_select_dark_btn, vibrantColor)
                setButtonColor(fragment_settings_theme_select_custom_btn, vibrantColor)

            }
        }

        // Theme changing buttons
        fragment_settings_theme_select_dark_btn.setOnClickListener {
            intendedSettingsPause = true
            saveTheme(context!!, "dark")
            activity!!.recreate()
        }
        fragment_settings_theme_select_finn_btn.setOnClickListener {
            intendedSettingsPause = true
            saveTheme(context!!, "finn")
            activity!!.recreate()
        }
        fragment_settings_theme_select_custom_btn.setOnClickListener {
            intendedSettingsPause = true
            // Request permission (on newer APIs)
            if (Build.VERSION.SDK_INT >= 23) {
                when {
                    ContextCompat.checkSelfPermission(context!!,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    -> letUserPickImage()
                    shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    -> {}
                    else
                    -> requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION_STORAGE)
                }
            }
            else letUserPickImage()
        }

        super.onStart()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            REQUEST_PERMISSION_STORAGE -> letUserPickImage()
            REQUEST_PICK_IMAGE -> handlePickedImage(resultCode, data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /** Extra functions */

    private fun letUserPickImage(crop: Boolean = false) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_PICK // other option: Intent.ACTION_GET_CONTENT
        if (crop) intent.putExtra("crop", "true")
        intendedSettingsPause = true
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    private fun handlePickedImage(resultCode: Int, data: Intent?) {

        if (resultCode == AppCompatActivity.RESULT_OK) {
            if (data == null) return

            val imageUri = data.data
            background = MediaStore.Images.Media.getBitmap(context!!.contentResolver, imageUri)

            Palette.Builder(background!!).generate {
                it?.let { palette ->
                    dominantColor = palette.getDominantColor(ContextCompat.getColor(context!!, R.color.darkTheme_accent_color))
                    vibrantColor = palette.getVibrantColor(ContextCompat.getColor(context!!, R.color.darkTheme_accent_color))

                    // never let dominantColor equal vibrantColor
                    if(dominantColor == vibrantColor) {
                        vibrantColor = manipulateColor(vibrantColor, 1.2F)
                        dominantColor = manipulateColor(dominantColor, 0.8F)
                    }

                    /* Save image Uri as string */
                    val editor: SharedPreferences.Editor = context!!.getSharedPreferences(
                        context!!.getString(R.string.preference_file_key), Context.MODE_PRIVATE).edit()
                    editor.putString("background_uri", imageUri.toString())
                    editor.putInt("custom_dominant", dominantColor)
                    editor.putInt("custom_vibrant", vibrantColor)
                    editor.apply()

                    saveTheme(context!!, "custom")
                    intendedSettingsPause = true
                    activity!!.recreate()
                }
            }
        }
    }
}