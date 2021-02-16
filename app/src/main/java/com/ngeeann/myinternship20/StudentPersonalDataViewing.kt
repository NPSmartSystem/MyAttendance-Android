package com.ngeeann.myinternship20

import android.app.DatePickerDialog
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.ngeeann.myinternship20.databinding.StudentPersonaldataViewingBinding
import kotlinx.android.synthetic.main.student_personaldata_viewing.*
import java.util.*
import kotlin.collections.ArrayList

/*
Student Personal Attendance Viewer
1. Selected day chooses which day's attendance records they would like to view
2. Send a query (querySchedule()) to the database, looking for the user's daily schedule for the selected day of the week and retrieves the values e.g. Tuesday has LNXSR, CCADC & BASRS
    path: users/[userid]/Modules/[day of week] and then initializes values into a list for the day.
3. queryAttendance() works by path-ing to the specific date and then making use of an algorithm to find lessons that have the user's attendance listed in them
   DB query path: db/attendance/[specific date]
4. Displays all values in Step 2 lesson list, filling in with information from step 3 for lessons that were found and defaulting to "Absent" for those that were not
5. User can pick another day from the same page, change the date sends a new query to the database and repeating steps 2, 3 & 4 with that date's information

Step 3 algorithm works by attempting to query for a module found in the moduleNameGroupListArray. If it does find it, it will fill in the information into the 2 arrays, entryTimeListArray
and statusLIstArray. If it doesn't, the program will assume that the student failed to show up for lessons and count them as absent.
 */

class StudentPersonalDataViewing : AppCompatActivity(), DatePickerDialog.OnDateSetListener {
    private lateinit var binding: StudentPersonaldataViewingBinding
    private lateinit var userId: String

    private val cal = Calendar.getInstance()
    private val database = Firebase.database.reference
    private val tag = "StudentAttendance" //A log tag declaration for debugging usage. Search for "StudentAttendance" in Logcat to find log entries for the program

    private var chosenDate = 0
    private var chosenMonth = 0
    private var chosenYear = 0

    //This group of arrays is for the schedule. Due to the RecyclerViews nature, it is required to place some default values in to prevent the application from taking a walk off a cliff.
    var moduleNameGroupListArray = arrayListOf(
        "Module 1 - Class 1",
        "Module 2 - Class 2",
        "Module 3 - Class 3",
        "Module 4 - Class 4",
        "Module 5 - Class 5"
    ) //Array of modules and group hybrid names
    var startAtListArray = arrayListOf(
        "startTime1",
        "startTime2",
        "startTime3",
        "startTime4",
        "startTime5"
    ) //Array of class start times
    var endAtListArray = arrayListOf(
        "endTime1",
        "endTime2",
        "endTime3",
        "endTime4",
        "endTime5"
    ) //Array of class end times
    var locationListArray = arrayListOf(
        "location 1",
        "location 2",
        "hbl",
        "location 3",
        "hbl",
        "hbl"
    ) //Array of class locations

    //^ These 4 array lists are used for querySchedule while the 2 below v are used for queryAttendance. All 6 are cleared out during the function set call
    var entryTimeListArray = arrayListOf(
        "entryTime1",
        "entryTime2",
        "entryTime3",
        "entryTime4",
        "entryTime5"
    ) //Array of Student's attendance times
    var statusListArray = arrayListOf(
        "Present",
        "Late",
        "Absent",
        "NA",
        "NA"
    ) //Array of the student's lesson attendance statuses, Present, Late, MC and Absent.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = intent.getStringExtra("userId").toString()
        binding = StudentPersonaldataViewingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getDateCalendar() //gets date of today
        querySchedule(userId, changeDayToString(cal.get(Calendar.DAY_OF_WEEK)))

        binding.studentAttendanceRecyclerView.layoutManager =
            LinearLayoutManager(this) //vertical layout for recycler view

        binding.StudentDataBackArrow.setOnClickListener { //go back to dashboard
            this.finish()
        }

        binding.dateTextView.setOnClickListener { //
            pickDate()
            querySchedule(userId, changeDayToString(cal.get(Calendar.DAY_OF_WEEK)))
        }

        binding.studentAttendanceRecyclerView.adapter =
            StudentAttendanceRecyclerAdapter() //resets recycler data shown

        binding.dayDisplayTextView.setOnClickListener { //allows user to open date picker
            pickDate()
            querySchedule(userId, changeDayToString(cal.get(Calendar.DAY_OF_WEEK)))
        }

        binding.dateNextButton.setOnClickListener { //goes to the next day by 1
            cal.add(Calendar.DATE, 1)
            getDateCalendar()
            querySchedule(userId, changeDayToString(cal.get(Calendar.DAY_OF_WEEK)))
        }

        binding.datePreviousButton.setOnClickListener { //goes to the previous day by 1
            cal.add(Calendar.DATE, -1)
            getDateCalendar()
            querySchedule(userId, changeDayToString(cal.get(Calendar.DAY_OF_WEEK)))
        }
    } //end of activity code


    private fun changeMonthToString(monthNum: Int): String { //to change the month Int to Strings
        val monthArray = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return monthArray[monthNum]
    }

    private fun changeDayToString(dateNum: Int): String { //to change the dayOfWeek Int to String
        val dateArray = arrayOf(
            "",
            "Sunday",
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday"
        )
        return dateArray[dateNum]
    }

    private fun getDateCalendar() { //gets the date and sets the date into text View
        chosenDate = cal.get(Calendar.DAY_OF_MONTH)
        chosenMonth = cal.get(Calendar.MONTH)
        chosenYear = cal.get(Calendar.YEAR)

        binding.dateTextView.text = "$chosenDate ${changeMonthToString(chosenMonth)} $chosenYear"
        binding.dayDisplayTextView.text = changeDayToString(cal.get(Calendar.DAY_OF_WEEK))
    }

    private fun pickDate() { //opens date picker
        getDateCalendar()
        DatePickerDialog(this, this, chosenYear, chosenMonth, chosenDate).show()
    }

    override fun onDateSet(
        view: DatePicker?,
        year: Int,
        month: Int,
        dayOfMonth: Int
    ) { //on date picker change date

        chosenDate = dayOfMonth
        chosenMonth = month
        chosenYear = year

        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        binding.dateTextView.text = "$chosenDate ${changeMonthToString(chosenMonth)} $chosenYear"
        binding.dayDisplayTextView.text = changeDayToString(cal.get(Calendar.DAY_OF_WEEK))
    }

    inner class StudentAttendanceRecyclerAdapter :
        RecyclerView.Adapter<StudentAttendanceRecyclerAdapter.ViewHolder>() { //the adapter for the recycler view

        //makes the items in recyclerview fill with the StaffStudentDataCard. Each item will be the card instead of "item 1" and so on. Ignore this. This is for layout purposes only
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): StudentAttendanceRecyclerAdapter.ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.student_attendanceviewing_card, parent, false)
            return ViewHolder(v)
        }

        /*
        places the data into the respective item views for student_attendanceviewing_card.xml
                moduleGroupLabel<TextView> --> "64CCADC-PB13"
                classTime<TextView> --> "13:00 - 14:00"
                status<TextView> --> "Absent"
                location<TextView> --> "Blk 5 #03-02"
                time<TextView> --> "13:05" (entry time)
                cardView<constraintLayout> --> It's the layout where all the details are in. Used to change the color.
        */

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.moduleGroupLabel.text = moduleNameGroupListArray[position]
            holder.classTime.text = "${startAtListArray[position]}-${endAtListArray[position]}"
            holder.status.text = statusListArray[position]
            holder.location.text = locationListArray[position]

            if (statusListArray[position] == "Late") {
                holder.time.visibility = View.VISIBLE
            }

            holder.time.text = entryTimeListArray[position]
            //changes color based on the status
            when (statusListArray[position]) {
                "Late" -> {
                    holder.cardView.setBackgroundColor(Color.parseColor("#A22222")) //red
                }
                "Absent" -> {
                    holder.cardView.setBackgroundColor(Color.parseColor("#292929")) //gray
                }
                "Present" -> {
                    holder.cardView.setBackgroundColor(Color.parseColor("#26B357")) //green
                }
            }
        }

        override fun getItemCount(): Int { //size always takes from student's daily schedule, never from attendance logs
            return moduleNameGroupListArray.size //this will give the total number of items in array. So 5 items will give int of 4
        }

        inner class ViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) { //initializing of what values will be placed in which textView.

            var moduleGroupLabel: TextView = itemView.findViewById(R.id.moduleGroup_card)
            var classTime: TextView = itemView.findViewById(R.id.classTime_card)
            var status: TextView = itemView.findViewById(R.id.statusTextView)
            var location: TextView = itemView.findViewById(R.id.classLocation_card)
            var time: TextView = itemView.findViewById(R.id.entryTime_card)
            var cardView: ConstraintLayout = itemView.findViewById(R.id.studentAttendanceCardView)

        }

    } //end of adapter

    /* I'm mothballing this function because while the attendance query could benefit from it by reducing calculation times, I haven't found a proper way to integrate it in. - Daniel
    fun findCommonElements (arr1: ArrayList<String>, arr2: ArrayList<String>): Array<String?> { //finds common elements in both arraylists, this is normal and acceptable
        // returns an array with the common values
        val hash1 = HashSet(arr1)
        val hash2 = HashSet(arr2)
        val finArray = arrayOfNulls<String>(hash1.size)

        hash1.retainAll(hash2) //checks elements in array2, keep matching elements
        hash1.toArray(finArray) //converts hashmap hash1 to a String array
        return finArray //returns array
    }
    */

    private fun conDateToString(): String { //Format of YYYY-MM-DD
        val month = if ((chosenMonth + 1) < 10) {
            "0${chosenMonth + 1}"
        } else {
            "${chosenMonth + 1}"
        }
        val date = if (chosenDate < 10) {
            "0$chosenDate"
        } else {
            chosenDate
        }
        return "$chosenYear-$month-$date"
    }

    private fun querySchedule(userId: String, dayOfWeek: String) {
        Log.d(tag, "Starting querySchedule function. Day of week is $dayOfWeek")
        val path = database.child("users/$userId/Modules/$dayOfWeek").orderByChild("StartAt")
        path.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                moduleNameGroupListArray.clear()
                locationListArray.clear()
                startAtListArray.clear()
                endAtListArray.clear()

                for (modSnapshot in snapshot.children) {
                    moduleNameGroupListArray.add(modSnapshot.key.toString()) //e.g. 64CCADC-LB12 [Module-Group]
                    Log.i(tag, "Module: ${modSnapshot.key.toString()}")
                    locationListArray.add(modSnapshot.child("Location").value.toString())
                    startAtListArray.add(modSnapshot.child("StartAt").value.toString())
                    endAtListArray.add(modSnapshot.child("EndAt").value.toString())
                }
                queryAttendance(
                    userId,
                    conDateToString(),
                    moduleNameGroupListArray
                ) //as the required information is only supplied at the end of querySchedule, the second function is called at the end
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(tag, "Schedule Query Failed")
            }
        })
    }


    private fun queryAttendance(userId: String, date: String, modListArray: ArrayList<String>) {
        val path = database.child("attendance/$date").orderByChild("$userId/EntryTime")

        path.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { //initializes data into array, checks for matches with moduleNameListArray, detailListArray gets filled with information for each one
                entryTimeListArray.clear()
                statusListArray.clear()
                for (i in modListArray) { //these 3 lists will be added to if the student was present for class
                    if (snapshot.child("$i/$userId").exists()) {
                        Log.i(tag, "Lesson $i found")
                        entryTimeListArray.add(snapshot.child("$i/$userId/EntryTime").value.toString())
                        statusListArray.add(snapshot.child("$i/$userId/Status").value.toString())
                    } else {
                        entryTimeListArray.add("")
                        statusListArray.add("Absent")
                    }
                }
                studentAttendanceRecyclerView.adapter?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(tag, "Attendance Query Failed")
            }
        })
    }
}