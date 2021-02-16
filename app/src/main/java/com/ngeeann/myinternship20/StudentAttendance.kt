package com.ngeeann.myinternship20

import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.student_attendance.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

/*
Feature code process
Student Mode
1) Checks if there is any upcoming or current lessons for the student with the checkClass function
2) If there is an upcoming lesson in 15 minutes or current lesson, display it on the screen and instantiate currentLesson object from the Lesson class
3) If the lesson is upcoming, disable both buttons and skip steps 4 to 6. If the lesson is taking place at the current time continue on to step 4
4) Checks for their attendance status (Present, Absent, Late or MC) with checkAttendance function
5) Wait for User to press Attend or MC button
6) Attend button press triggers attendanceAttendButton's OnClickListener function, creating an entry for that day with information such as the time they checked-in,
their name and their attendance status (Present or Late)
6a) MC button press triggers attendanceMCButton's OnClickListener function, creating an entry for that day with information such as the link to a picture of the MC,
their name and their attendance status (MC)
Intern Mode (Intern's should be able to check in or out whenever as work days may not always start or end at a defined time,
thus I plan to skip the original checkClass function for this)
1) Checks for Intern's current attendance status with checkAttendance function and instantiates currentLesson object from the Lesson class
2) If user has not checked in for today allow them to press Check-in button
3) Attend button press triggers attendanceAttendButton's OnClickListener function, creating an entry for that day with information such as the time they checked-in,
their name and their attendance status (Present or Late)
3a) MC button press triggers attendanceMCButton's OnClickListener function, creating an entry for that day with information such as the link to a picture of the MC,
their name and their attendance status (MC) and skip steps 4 to 5.
4) Check-out button will take the place of the Check-in button if(userType == "Intern" && currentLesson.Status != MC)
5) Pressing the check-out button will trigger it's OnClickListener function, changing the user's status to Checked-Out and adding a LeaveTime child node in the entry
*/

class StudentAttendance : AppCompatActivity() {
    private val database = Firebase.database
    private val tag: String = "StudentAttendance"
    private lateinit var userType: String
    lateinit var currentLesson: Lesson
    lateinit var userName: String

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_attendance)
        val userId = intent.getStringExtra("userId") //fetches userId from the main screen to use for querying database
        userName = intent.getStringExtra("username").toString()
        userType= intent.getStringExtra("group").toString() //Student or Intern
        val calendar = Calendar.getInstance()
        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) //Date format: yyyy-MM-dd, 2020-12-25
        val time = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME) //Time format: 17:12:47

        val weekArray = arrayOf("", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday") //array to convert numeral value of
        // day of the week to the string name
        val dayOfWeek = weekArray[calendar.get(Calendar.DAY_OF_WEEK)]
        if (userType == "Student") {
            checkClass(userId.toString(), dayOfWeek, time) //WHEN TESTING MAKE SURE THE HOUR PORTION HAS 2 DIGITS e.g. 8 am is 08:00
        }
        else {
            checkIntern(userId.toString(), dayOfWeek)
        }

        attendanceBackArrow.setOnClickListener { //returns back to UIStudent/UIIntern main screen
            this.finish()
        }

        attendanceAttendButton.setOnClickListener { //should only be clickable when lesson has started (startTime <= localtime < endTime), special mode for interns
            if(attendText.text == "ATTEND") {
                if (LocalTime.now() <= currentLesson.startTime.plusMinutes(5)) { //that means its "On Time" (within 5 minutes)
                    currentLesson.status = "Present"
                } else {
                    currentLesson.status = "Late"
                }
                currentLesson.entryTime = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME) //the current time when the attendance button was pressed
                //currentLesson.details = time
                attendUpdate(userId.toString(), date)
                attendButtonClicked()
            }
            else { //LEAVE Button is technically the attendanceAttendButton that will do a different set of instructions
                database.getReference("attendance/$date/${currentLesson.title}/$userId").child("LeaveTime")
                        .setValue(LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)) //logs the time when user pressed the leave button
                attendButtonClicked()
                attendanceLateWarning.text = "Status: Clocked Off"
            }
        }

        attendanceMCButton.setOnClickListener { //TODO advanced MC button will involve adding in the file uploading thing and then mark all lessons for the day with MC
            currentLesson.status = "MC"
            //currentLesson.details = "MC PLACEHOLDER LINK"
            attendUpdate(userId.toString(), date)
            attendButtonClicked()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkClass(userId: String, dayOfWeek: String, time: String) { //uses day of the week and current time to check if there's an ongoing class
        val path = database.getReference("users/$userId/Modules/$dayOfWeek").orderByChild("EndAt").startAt(time) //checks for any classes that
        // end after the given time, only allows current/upcoming classes to show up in the snapshot
        val localTime = LocalTime.parse(time) //converts the Time string value into a Time LocalTime unit value

        path.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChildren()) { //Lessons for that day exists, can apply to any day of the week as long as it there are lessons
                    var currLess = 0 //0 is the default value as it is also the first lesson in the day
                    val nameArr = arrayOfNulls<String>(snapshot.childrenCount.toInt())
                    val locArr = arrayOfNulls<String>(snapshot.childrenCount.toInt())
                    val grpArr = arrayOfNulls<String>(snapshot.childrenCount.toInt())

                    for ((n, classSnapshot) in snapshot.children.withIndex()) { //for-loop cycles through the multiple children in the snapshot as there may be multiple
                        // lessons in a single day
                        val startAt = classSnapshot.child("StartAt").getValue<String>() //can merge into the LocalTime Parser reducing the amount of memory consumed
                        val endAt = classSnapshot.child("EndAt").getValue<String>()
                        val startTime = LocalTime.parse(startAt)
                        val endTime = LocalTime.parse(endAt)

                        classSnapshot.child("Name").getValue<String>().also { nameArr[n] = it.toString() }
                        classSnapshot.child("Location").getValue<String>().also { locArr[n] = it.toString() }
                        classSnapshot.child("Group").getValue<String>().also { grpArr[n] = it.toString() }

                        if (localTime < startTime) { //checks if there is an upcoming lesson
                            if(localTime >= startTime.minusMinutes(15) ){
                                val diffTime = Duration.between(localTime, startTime).toMinutes()
                                attendanceStatus.text = "UPCOMING IN $diffTime MINUTES"
                                currLess = n
                                attendanceLinear.background = null
                                attendText.setTextColor(Color.parseColor("#B5B5B5"))
                                attendanceAttendButton.isClickable = false
                                currentLesson = Lesson(grpArr[currLess], nameArr[currLess], startTime)
                            } else { //If next lesson is upcoming but is more than 15 minutes away
                                currentLesson = Lesson(grpArr[0], nameArr[0], startTime)
                            }
                        } else if (localTime in startTime..endTime) { //checks if current time is in the middle of a lesson
                            attendanceStatus.text = "CURRENTLY ATTENDING"
                            currLess = n
                            currentLesson = Lesson(grpArr[currLess], nameArr[currLess], startTime) //instantiates the Lesson class object currentLesson
                        }
                    }
                    attendanceSet.text = nameArr[currLess] //displays only the lesson which either 1) Will commence in 15 minutes or 2) Is ongoing
                    attendanceLocation.text = locArr[currLess]
                    currentLesson.userName = userName
                    currentLesson.title = "${currentLesson.name.toString()}-${currentLesson.group}" //Title: 64BASRS-TB11
                    checkAttendance(userId, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                }
                else { //a third state that only appears when there are no lessons found within the query parameters e.g. lessons have ended for the day or no lessons
                    attendanceStatus.text = "Great job"
                    if (dayOfWeek == "Friday" || dayOfWeek == "Saturday"){ //Since there shouldn't be school or work on the next day (Saturday & Sunday)
                        attendanceSet.text = "See you next week!"
                    }
                    else {
                        attendanceSet.text = "See you tomorrow!"
                    }
                    attendanceLocation.text = ""
                    attendanceAttendButton.isVisible = false
                    attendanceMCButton.isClickable = false
                    attendanceLateWarning.text = ""
                    attendanceMCButton.isVisible = false
                    attendanceMCButton.isClickable = false
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(tag, "Unable to query database for lesson information.")
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun checkIntern(userId: String, dayOfWeek: String) {
        if (dayOfWeek != "Saturday" || dayOfWeek != "Sunday") {//checks if current day is a weekday or weekend
            val path = database.getReference("users/$userId/Modules/Internship")
            val time = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
            val localTime = LocalTime.parse(time)
            path.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val startTime = LocalTime.parse(snapshot.child("StartAt").getValue<String>().toString())
                    val endTime = LocalTime.parse(snapshot.child("EndAt").getValue<String>().toString())
                    currentLesson = Lesson("", "Internship", startTime)

                    if (localTime < startTime) { //checks if there is an upcoming lesson
                        if(localTime >= startTime.minusMinutes(15) ){ //condition that appears when there is less than 15 minutes before work officially starts
                            val diffTime = Duration.between(localTime, startTime).toMinutes()
                            attendanceStatus.text = "UPCOMING IN $diffTime MINUTES"
                            attendanceLinear.background = null
                            attendText.setTextColor(Color.parseColor("#B5B5B5"))
                            //attendanceAttendButton.isClickable = false
                        }
                    } else if (localTime in startTime..endTime) { //checks if current time is in the middle of a lesson
                        attendanceStatus.text = "CURRENTLY ATTENDING"
                    }

                    attendanceSet.text = "Internship" //displays only the lesson which either 1) Will commence in 15 minutes or 2) Is ongoing
                    attendanceLocation.text = snapshot.child("Location").getValue<String>().toString()
                    currentLesson.userName = userName
                    currentLesson.title = currentLesson.name.toString() //Title: Internship
                    checkAttendance(userId, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(tag, "Unable to query database for internship information.")
                }
            })
        }
        else { //Only appears when its a weekend
            attendanceStatus.text = "Great job"
            if (dayOfWeek == "Saturday"){
                attendanceSet.text = "See you next week!"
            }
            else {
                attendanceSet.text = "See you tomorrow!"
            }
            attendanceLocation.text = ""
            attendanceAttendButton.isVisible = false
            attendanceMCButton.isClickable = false
            attendanceLateWarning.text = ""
            attendanceMCButton.isVisible = false
            attendanceMCButton.isClickable = false
        }
    }

    fun checkAttendance(userId: String, date: String) { //checks if attendance has already been marked for the current class
        val path = database.getReference("attendance/$date/${currentLesson.title}").child(userId)
        path.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){ //allow User to press attendance button and informs user of their current attendance status: Present, Late, Absent or MC
                    val attendStat = snapshot.child("Status").getValue<String>()
                    if(snapshot.child("Status").getValue<String>() == "Absent") {
                        attendanceLateWarning.text = "Please mark your attendance within 5 minutes of class starting!"
                    } else if (currentLesson.title == "Internship" && snapshot.child("Status").getValue<String>() == "Present") { //This part will allow
                        // Interns to check out of work as some of their days might end earlier or later than usual
                        attendText.text = "LEAVE"
                        attendanceMCButton.isVisible = false  //hides the MC button since they've already clocked in
                        if(snapshot.child("LeaveTime").exists()){ //Will only trigger when the intern has clocked off work for the day
                            attendButtonClicked()
                            attendanceLateWarning.text  = "Status: Clocked Off"
                            attendanceLateWarning.setTextColor(Color.parseColor("#0ae973"))
                        }
                    }
                    else {
                        attendanceLateWarning.text = "Attendance Status: $attendStat"
                        attendanceLateWarning.setTextColor(Color.parseColor("#0ae973"))
                        attendButtonClicked()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(tag, "Unable to query database for attendance information.")
            }
        })
    }

    private fun attendButtonClicked () {
        if(userType == "Student" || attendText.text == "LEAVE") {
            attendanceLinear.background = null
            attendText.setTextColor(Color.parseColor("#B5B5B5"))
            attendanceAttendButton.isClickable = false //once attendance has been submitted, both buttons will be disabled for that lesson
            mcLinear.background = null
            mcText.setTextColor(Color.parseColor("#B5B5B5"))
            attendanceMCButton.isClickable = false
        }
        else {
            attendText.text = "LEAVE"
        }
    }

    private fun attendUpdate (userId: String, date: String) {
        val lessonValues: Map<String, Any?> = if (currentLesson.status == "MC"){
            currentLesson.toMCMap() //Contains MC Link, Name of User and Status of User (MC)
        }
        else {
            currentLesson.toMap() //Contains Time when Attendance was recorded, Name of User and Status of User (MC)
        }

        val attendUpdates = hashMapOf<String, Any>(
                userId to lessonValues
        )

        database.getReference("attendance/$date/${currentLesson.title}").updateChildren(attendUpdates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Attendance submitted", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener{
                    Toast.makeText(this, "Attendance was unable to submit please try again.", Toast.LENGTH_SHORT).show()
                }
        attendanceLateWarning.text = "Attendance Status: ${currentLesson.status}"
        attendanceLateWarning.setTextColor(Color.parseColor("#0ae973"))
    }

    data class Lesson (
            var group: String? = "",
            var name: String? = "",
            var startTime: LocalTime
    ) {
        var entryTime: String = "" //Value initialized in attendanceAttendButton.onClickListener
        var title: String = "" //Value initialized in checkClass function, two types, one will contain Lesson Name only while the other will contain Lesson Name+Group
        var userName: String = "" //Value initialized in checkClass function
        var status: String = "" //Student's attendance status: Present, Late, Absent or MC
        private var mc: String = "" //Value initialized in MCButton, Medical Certificate picture online database storage link// TODO add in a proper link later on (LOW PRIORITY)
        //var details: String = ""

        fun toMap(): Map<String, Any?> {
            return mapOf(
                    "EntryTime" to entryTime, //possible issue trying to convert map value of LocalTime
                    //"EntryTime to details, //to be used as a replacement of entryTime and merge the toMap() and toMCMap() functions
                    "Name" to userName,
                    "Status" to status
            )
        }

        fun toMCMap(): Map<String, Any?>{ //Map contains a link to the MC instead of an entry time since the student will not be attending class
            return mapOf(
                    "MC Link" to mc,
                    //"MC Link" to details, read line 205 comment
                    "Name" to userName,
                    "Status" to status
            )
        }
    }
}