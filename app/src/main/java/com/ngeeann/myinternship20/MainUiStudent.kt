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
import kotlinx.android.synthetic.main.ui_student_main.*

class MainUiStudent : AppCompatActivity() {
    val database = Firebase.database
    lateinit var userId: String
    lateinit var userName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ui_student_main)
        userId = intent.getStringExtra("userId").toString()
        fetchUserInfo(userId)

        studentAttendance.setOnClickListener {
            startActivity(Intent(this, StudentAttendance::class.java)
                    .putExtra("userId", userId)
                    .putExtra("username", userName)
                    .putExtra("group", "Student"))
        }

        studentData.setOnClickListener {
            startActivity(Intent(this, StudentPersonalDataViewing::class.java)
                    .putExtra("userId", userId))
        }
    }

    private fun fetchUserInfo(userId: String) { //checks for existing log for today using user ID & current date
        val path = database.getReference("users/$userId")

        studentNameText.text = userId
        path.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val student = snapshot.getValue<User>()
                student?.let{
                    studentNameText.text= it.Name + "'s Dashboard"
                    userName = it.Name.toString()
                    studentIdText.text = it.StudID
                    studentSchoolText.text = it.School
                    studentCourseText.text = it.Course
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(baseContext,"Unable to connect to the server.", Toast.LENGTH_SHORT).show()
                Log.w("Student Menu", "Failed to query database.")
            }
        })
    }

    data class User (
        var Name: String? = "",
        var StudID: String? = "",
        var School: String? = "",
        var Course: String? = ""
    )
}