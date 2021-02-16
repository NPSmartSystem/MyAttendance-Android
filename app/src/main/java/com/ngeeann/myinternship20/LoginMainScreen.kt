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
import kotlinx.android.synthetic.main.loginscreen.*

/*
1. When Login Button is pressed, system runs a check for empty idTextBoxes or empty PasswordTextBoxes
2. Calls loginQuery() which queries the database for the user's password and checks for a match
3. If the user entered and database passwords match, start groupQuery() which queries the database for the user's group
4. Start the activity that matches the users group, i.e group = student, launches MainUiStudent
 */
class LoginMainScreen : AppCompatActivity() {

    val database = Firebase.database
    val tag = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loginscreen)

        setupButton.setOnClickListener {
            val setup = LoginGroupSelect()
            setup.show(supportFragmentManager, "setup1")
        }
        loginButton.setOnClickListener {
            if(idTextBox.text.toString() == "" || passwordTextBox.text.toString() == ""){ //checks if textboxes are blank
                Toast.makeText(this,"ID or Password not entered", Toast.LENGTH_SHORT).show()
            }
            else{
                loginQuery()
            }
        }
    }

    private fun loginQuery() {
        val username = idTextBox.text   // gets string value from the on-screen idTextBox textview
        val password = passwordTextBox.text.toString() //gets string value from on the on-screen password text box
        val pwRef = database.getReference("users/$username/password")

        pwRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val dbPassword = snapshot.getValue<String>()
                    if (dbPassword == password) {
                        Toast.makeText(baseContext, "Login successful.",
                                Toast.LENGTH_SHORT).show()
                        grpQuery()
                    } else {
                        Toast.makeText(baseContext, "Please enter the correct password.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    Toast.makeText(baseContext, "Username not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(baseContext, "There was an error logging in, please try again.",
                    Toast.LENGTH_SHORT).show()
                Log.w(tag, "Login query failed.")    //log error message
            }
        })
    }

    private fun grpQuery() {
        val userId = idTextBox.text.toString()
        val grpRef = database.getReference("users/$userId/group")

        val studIntent = Intent(this, MainUiStudent::class.java).putExtra("userId",userId)
        val intIntent = Intent(this, MainUiIntern::class.java).putExtra("userId",userId)
        val npisIntent = Intent(this, MainUiNPIS::class.java).putExtra("userId",userId)
        val staffIntent = Intent(this, MainUiStaff::class.java).putExtra("userId",userId)

        grpRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                when (dataSnapshot.getValue<String>().toString()) {
                    "student" ->  startActivity(studIntent) //start Main_UI_Student activity
                    "intern" -> startActivity(intIntent) //start Main_UI_Intern activity
                    "npis" -> startActivity(npisIntent)//start Main_UI_NPIS activity
                    else -> {
                        startActivity(staffIntent) //start Main_UI_Staff activity
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(baseContext, "There was an error fetching the group value, please try again.",
                        Toast.LENGTH_SHORT).show() //ERROR MESSAGE
                Log.w(tag, "Group query failed.")
            }
        })
    }
}