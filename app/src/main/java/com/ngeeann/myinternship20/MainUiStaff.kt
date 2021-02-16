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
import kotlinx.android.synthetic.main.ui_staff_main.*

class MainUiStaff : AppCompatActivity() { //TODO add in the putExtra values later on to allow staff to check student attendance
    private val database = Firebase.database
    lateinit var userId: String
    val TAG = "Staff Menu"
    var staffModules: ArrayList<String?> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ui_staff_main)

        userId = intent.getStringExtra("userId").toString()

        fetchUserInfo(userId)

        staffStudentAttendanceData.setOnClickListener {
            startActivity(Intent(this,StaffAttendanceDataHome::class.java)
                    .putExtra("userId", userId)
                    .putExtra("moduleArray", staffModules))
        }

    }


    private fun fetchUserInfo(userId: String) { //checks for existing log for today using user ID & current date
        val path = database.getReference("users/$userId")
        path.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val staff = snapshot.getValue<User>()
                staff?.let{
                    staffNameText.text = it.Name + "'s Dashboard"
                    staffEmailText.text = it.email
                    staffRoleText.text = "NGEE ANN POLY, " + it.group
                    staffSchoolText.text = it.school
                }
                for (childModules in snapshot.child("Modules").children) {
                    staffModules.add(childModules.key.toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(baseContext,"Unable to connect to the server.", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Failed to query database.")
            }
        })
    }

    data class User (
        var Name: String? = "",
        var school: String? = "",
        var email: String? = "",
        var group: String? = ""
    )
}