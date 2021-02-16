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
import kotlinx.android.synthetic.main.ui_npis_main.*

class MainUiNPIS : AppCompatActivity() {
    private val database = Firebase.database
    lateinit var userId: String
    val TAG = "NPIS Menu"
    var studentName: ArrayList<String?> = arrayListOf()
    var studentId: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ui_npis_main)
        userId = intent.getStringExtra("userId").toString()

        fetchUserInfo(userId) //gets supervisor particular to display on the screen
        fetchStudentInfo(userId) //gets student info and puts it in an array

        npisStudentData.setOnClickListener{
            startActivity(Intent(this, NpisStudentDataHome::class.java)
                    .putExtra("userId", userId)
                    .putExtra("studNameArr", studentName)
                    .putExtra("studIdArr", studentId))
        }
    }


    private fun fetchUserInfo(userId: String) { //checks for existing log for today using user ID & current date
        val path = database.getReference("users/$userId")
        path.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val staff = snapshot.getValue<User>()
                staff?.let{
                    npisNameText.text = it.Name + "'s Dashboard"
                    npisEmailText.text = it.email
                    npisSchoolText.text = it.school
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(baseContext,"Unable to connect to the server.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchStudentInfo(userId: String) {
        val path = database.getReference("users/$userId/Supervising").orderByChild("Name")
        path.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (studentSnapshot in snapshot.children) {
                    studentName.add(studentSnapshot.child("Name").getValue<String>().toString())
                    studentId.add(studentSnapshot.child("userid").getValue<String>().toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG,"Failed to query database.")
            }
        })
    }

    data class User (
            var Name: String? = "",
            var school: String? = "",
            var email: String? = "",
            var group: String? =""
    )
}