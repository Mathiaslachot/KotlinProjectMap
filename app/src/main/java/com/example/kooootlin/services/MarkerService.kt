package com.example.kooootlin.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MarkerService {

    companion object {
        private val LOGTAG: String = SharedPreferences::class.java.name
        private val ID_PREF = "mathias"
        private val ALARM_TAG = "alarm_tag"

        // Get all the list of alarms
        fun getMarkers(context: Context): List<MarkerOptions> {
            val markers = ArrayList<MarkerOptions>()

            try {
                val mPrefs = context.getSharedPreferences(ID_PREF, Context.MODE_PRIVATE)
                val json = mPrefs.getString(ALARM_TAG, null)


                if (json != null) {

                    val gson = Gson()
                    markers.addAll(gson.fromJson<ArrayList<MarkerOptions>>(json, object: TypeToken<ArrayList<MarkerOptions>>() {}.type))
                }


            } catch (e: Exception) {
                Log.e("$LOGTAG getMarkers()", e.toString())
            }

            return markers
        }


        fun setMarkers(context: Context, markers: List<MarkerOptions>) {
            try {
                val mPrefs =
                    context.getSharedPreferences(ID_PREF, Context.MODE_PRIVATE)
                val prefsEditor = mPrefs.edit()
                val gson = Gson()
                prefsEditor.putString(ALARM_TAG, gson.toJson(markers))
                prefsEditor.apply()
            } catch (e: Exception) {
                Log.e("$LOGTAG setMarkers()", e.toString())
            }
        }

    }
}