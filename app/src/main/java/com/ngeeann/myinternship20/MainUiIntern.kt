package com.ngeeann.myinternship20

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.ui_intern_main.*

class MainUiIntern : AppCompatActivity() {
    private val database = Firebase.database
    lateinit var userId: String
    lateinit var username: String
    val TAG = "Intern Menu"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ui_intern_main)
        userId = intent.getStringExtra("userId").toString()
        fetchUserInfo(userId)

        internLog.setOnClickListener {
            startActivity(Intent(this, InternLog::class.java)
                    .putExtra("userId", userId)
                    .putExtra("username", username))
        }

        internAttendance.setOnClickListener {
            startActivity(Intent(this, StudentAttendance::class.java)
                    .putExtra("userId", userId)
                    .putExtra("username", username)
                    .putExtra("group", "Intern"))
        }

        internData.setOnClickListener {
            startActivity(Intent(this, InternPersonalDataView::class.java)
                    .putExtra("userId", userId))
        }
    }

    private fun fetchUserInfo(userId: String) { //fetches user's particulars using their username, by right userId should be the Student Number (S102XXXXXX)
        // but for testing it is a name
        val path = database.getReference("users/$userId")

        path.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val intern = snapshot.getValue<User>()
                intern?.let{
                    internNameText.text = it.Name + "'s Dashboard"
                    username = it.Name.toString()
                    internIdText.text = it.StudID
                    internSchoolText.text = it.School
                    internCourseText.text = it.Course
                    internAddressText.text = it.address
                    internPostalText.text = "SG " + it.postal
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(baseContext,"Unable to connect to the server.",Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Failed to query database.")
            }
        })
    }

    data class User (
        var Course: String? = "",
        var Name: String? = "",
        var StudID: String?= "",
        var School: String? = "",
        var address: String? = "",
        var postal: String? = ""
    )
}