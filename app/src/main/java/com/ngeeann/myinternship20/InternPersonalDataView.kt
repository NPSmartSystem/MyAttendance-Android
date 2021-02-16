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
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.ngeeann.myinternship20.databinding.InternPersonaldataViewBinding
import kotlinx.android.synthetic.main.intern_personaldata_view.*
import kotlinx.android.synthetic.main.intern_personaldata_view.customCalendar
import java.util.*

/* Personal Data viewing for Interns
1) Logbook feature is similar to the one found in the NPIS Logbook viewer but with minor tweaks to make it more personalized.

2) The attendance feature, appears in a monthly calendar format. The feature uses a set of arraylists to store information taken from the attendance records in the database.
   Days in the calendar are presented as squares on screen and the intern's daily attendance status is represented by a colour in this order:
   Status  |  Colour      |  Hex Code
   Present |  Green       |  #2ACC4C
   Late    |  Red         |  #CC1010
   MC      |  Dark Gray   |  #5A5A5A
   Absent* |  Light Gray  |  #FFFFFF
   * if no record is found, the system considers them as absent, this could mean an intent who did not mark their attendance or no work occurring on that day.

3) The calendar array needs spacing in front to showcase the first day of the month in the correct column. customCalendarDatesArrayList
   if it's on tuesday, the array list would be "","1"...so on til last day of the month
   so for example January 2020. 1st Jan is on friday. Therefore, this array would be {"","","","","","",1,2....,31}

   statusArrayList has the same kind of formatting. The spaces are created by the function "setFirstDayOfMonth". Inside it already clears
   the statusArrayList

   after the adding of spaces, retrieve the array of status using the customCalendarDatesArrayList. If it's possible. If no data is entered, make it null or ""

4) when the person chooses the specified date, it would showcase the date that was selected (because idk how to change color of a selected one and refresh it to the original color.
    the entry time and the leave time in a details layout. You can add more details to be added if you want.

If you want to use a function/variable inside the recyclerView, the function has to be public.
 */

class InternPersonalDataView : AppCompatActivity(), DatePickerDialog.OnDateSetListener {

    private lateinit var binding: InternPersonaldataViewBinding
    private lateinit var userId: String
    private val cal = Calendar.getInstance() //calendar for log viewing
    private val customCal = Calendar.getInstance() //calendar for custom calendar (yes I need 2)
    private val database = Firebase.database.reference
    private val TAG1 = "LogBookFeature"
    private val TAG2 = "AttendFeature"

    private var chosenDay = 0
    var chosenMonth = 0
    var chosenYear = 0

    //These lists are used for the Intern Log Viewer
    val logArrayList = arrayListOf<String>()
    val dateArrayList = arrayListOf<String>()

    //These lists are used for the attendance viewing feature
    var statusArrayList = arrayListOf<String>() //the array of status with "" same as customCalendar so that it can check the status and change the background accordingly
    val entryTimeArrayList = arrayListOf<String>()
    val leaveTimeArrayList = arrayListOf<String>()
    private val attendanceDatesArrayList = arrayListOf<String>()

    var customCalendarDatesArrayList = arrayListOf<String>()
    private var customCalMonth = 0 //custom Calendar focused on month arguments
    private var firstDayOfMonth = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        userId = intent.getStringExtra("userId").toString()
        super.onCreate(savedInstanceState)
        binding = InternPersonaldataViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Navigation between Log and Attendance
        binding.internBottomNav.setOnNavigationItemSelectedListener { item ->
            when(item.itemId){
                R.id.intern_log -> {
                    binding.internPersonalLogLayout.visibility = View.VISIBLE
                    binding.internPersonalAttendanceLayout.visibility = View.GONE
                }
                R.id.intern_attendance -> {
                    binding.internPersonalLogLayout.visibility = View.GONE
                    binding.internPersonalAttendanceLayout.visibility = View.VISIBLE
                    customCal.set(Calendar.MONTH,chosenMonth)
                }
            }
            true
        }

        //****code for Log viewing starts here****
        getDateCalendar() //called to get the day's details
        binding.dateSubmittedLogText.text = "$chosenDay ${changeMonthToString(chosenMonth)} $chosenYear"
        fetchStudentLog()
        customCal.set(Calendar.MONTH,chosenMonth)
        setFirstDayOfMonth(customCalMonth)
        setArrayOfDates(getLastDayOfMonth(customCalMonth))
        attendByMonth(userId)

        binding.dateSubmittedLogText.setOnClickListener { //brings up DatePickerDialogue for log
            pickDate()
        }

        binding.dateNextButton.setOnClickListener{ //moves to next date for log
            nextDay()
        }

        binding.datePrevButton.setOnClickListener { //moves to prev date for log
            prevDay()
        }



        //**** code for Attendance viewing starts here ****
        binding.customCalendar.layoutManager = GridLayoutManager(applicationContext,7,LinearLayoutManager.VERTICAL,false) //makes 7 columns for custom calendar
        binding.customCalendar.setHasFixedSize(true) //makes it so that the calendar doesn't deform
        binding.customCalendar.adapter = CustomCalendarAdapter() //putting in the adapter

        customCalMonth = customCal.get(Calendar.MONTH)
        setFirstDayOfMonth(customCalMonth)
        for (i in 1..31){
            statusArrayList.add("")
        }
        binding.calendarText.text = "${changeMonthToString(customCalMonth)} $chosenYear"

        setArrayOfDates(getLastDayOfMonth(customCalMonth))

        binding.nextMonthButton.setOnClickListener { //moves to the next month
            nextMonth()
        }

        binding.prevMonthButton.setOnClickListener { //moves to previous month
            prevMonth()
        }

        binding.internPersonalDataBackArrow.setOnClickListener { //back to dashboard
            this.finish()
        }
    }

    fun changeMonthToString(monthNum: Int): String { //function to change month to a string
        val monthArray = arrayOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        return monthArray[monthNum]
    }

    private fun getDateCalendar(){ //function to get date of calendar
        chosenDay = cal.get(Calendar.DATE)
        chosenMonth = cal.get(Calendar.MONTH)
        chosenYear = cal.get(Calendar.YEAR)
    }

    private fun pickDate() {//picks date and brings up the date picker
        getDateCalendar()
        DatePickerDialog(this,this, chosenYear, chosenMonth, chosenDay).show()
        customCal.set(Calendar.MONTH,chosenMonth)
        findLogByDate()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) { //datepicker function
        chosenDay = dayOfMonth
        chosenMonth = month
        chosenYear = year
        binding.dateSubmittedLogText.text = "$chosenDay ${changeMonthToString(chosenMonth)} $chosenYear"

        cal.set(Calendar.YEAR,year)
        cal.set(Calendar.MONTH,month)
        cal.set(Calendar.DAY_OF_MONTH,dayOfMonth)
        customCal.set(Calendar.MONTH,chosenMonth)
        findLogByDate()
    }

    private fun nextDay() { //goes to next day for log
        cal.add(Calendar.DATE,1)
        getDateCalendar()
        binding.dateSubmittedLogText.text = "$chosenDay ${changeMonthToString(chosenMonth)} $chosenYear"
        findLogByDate()
    }

    private fun prevDay() { //goes to prev day for log
        cal.add(Calendar.DATE,-1)
        getDateCalendar()
        binding.dateSubmittedLogText.text = "$chosenDay ${changeMonthToString(chosenMonth)} $chosenYear"
        findLogByDate()
    }

    private fun nextMonth() { //goes to the next month for attendance
        customCal.add(Calendar.MONTH,1)
        customCalMonth = customCal.get(Calendar.MONTH)
        binding.calendarText.text = "${changeMonthToString(customCal.get(Calendar.MONTH))} ${customCal.get(Calendar.YEAR)}"
        setFirstDayOfMonth(customCalMonth)
        setArrayOfDates(getLastDayOfMonth(customCalMonth))
        attendByMonth(userId)
    }

    private fun prevMonth() { //goes to the prev month for attendance
        customCal.add(Calendar.MONTH,-1)
        customCalMonth = customCal.get(Calendar.MONTH)
        binding.calendarText.text = "${changeMonthToString(customCal.get(Calendar.MONTH))} ${customCal.get(Calendar.YEAR)}"
        setFirstDayOfMonth(customCalMonth)
        setArrayOfDates(getLastDayOfMonth(customCalMonth))
        attendByMonth(userId)
    }


    //to set the calendarDatesArray to the correct dayOfWeek also clear statusArray and adds the space to make the positioning of conditions the same
    private fun setFirstDayOfMonth(mm: Int) {
        customCalendarDatesArrayList.clear()
        statusArrayList.clear()
        attendanceDatesArrayList.clear()

        customCal.set(Calendar.MONTH,mm)
        customCal.set(Calendar.DATE,1)

        firstDayOfMonth = customCal.get(Calendar.DAY_OF_WEEK)

        for (n in 2.. firstDayOfMonth) { //replacement for the giant when condition that has been commented out below
            customCalendarDatesArrayList.add("")
            statusArrayList.add("")
        }
    }

    private fun setArrayOfDates(lastDayOfMonth: Int){ //sets the array of dates into the arraylist and inserts it into the calendar
        for(n in 1..lastDayOfMonth){
            customCalendarDatesArrayList.add(n.toString()) //this one is for the calendar
            attendanceDatesArrayList.add(conDateToStringAttendanceVer(n))
        }
    }

    private fun getLastDayOfMonth(mm:Int): Int{ //to get the last day of the month
        customCal.set(Calendar.MONTH,mm)
        customCal.add(Calendar.MONTH,1)
        customCal.add(Calendar.DATE,-1)

        return customCal.get(Calendar.DATE)
    }

    private fun conDateToString(): String { //converts the currently selected date from integer values to a single combined string: "04 November 2020"
        val day = if(chosenDay < 10) { //appends a 0 in front of the day number e.g. 9 will be 09
            "0${chosenDay}"
        }
        else {
            chosenDay.toString()
        }
        return "$day ${changeMonthToString(chosenMonth)} $chosenYear"
    }

    private fun conDateToStringAttendanceVer(chosenDay: Int): String { //this one is for the attendance database. YYYY-MM-DD
        val day = if(chosenDay < 10) { //appends a 0 in front of the day number e.g. 9 will be 09
            "0${chosenDay}"
        }
        else {
            chosenDay.toString()
        }
        return "${customCal.get(Calendar.YEAR)}-${customCal.get(Calendar.MONTH)+1}-$day"
    }

    private fun fetchStudentLog() { //uses studentId to query for daily logs written by the student and places the date and log entry values into arrayLists
        val path = database.child("internlogs").orderByChild("userid").equalTo(userId)

        path.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    dateArrayList.clear() //clears out the arrayLists to add the date and logs of a different user TODO possibly switch the program to create another arrayList for each student to store values instead of clearing them
                    logArrayList.clear()
                    for (studentSnapshot in snapshot.children) {
                        dateArrayList.add(studentSnapshot.child("date").getValue<String>().toString())
                        logArrayList.add(studentSnapshot.child("log").getValue<String>().toString())
                    }
                    findLogByDate()
                }
                else {
                    personalLogDisplay.text = "Unable to find any entries."
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG1, "Query failed.")
            }
        })
    }

    fun findLogByDate () { //scans through dateArrayList to find an entry with a date that matches the selected calendar date and displays it on screen
        val selectedIndex = dateArrayList.indexOf(conDateToString())
        if (selectedIndex == -1) { // An indexOf return value of -1 means that the element specified in the brackets was unable to be found, so this condition will notify the user of this
            personalLogDisplay.text = "Unable to find any entries for this day."
        }
        else {
            personalLogDisplay.text = logArrayList[selectedIndex]
        }
    }

    fun attendByMonth (studId: String) { //instantiates all attendance records for the intern for the month
        val path = database.child("attendance").orderByKey() //orders the attendance records by student ID
        val month = if (customCalMonth < 9) {
            "0${customCalMonth + 1}"
        }
        else {
            customCalMonth + 1
        }
        val year = customCal.get(Calendar.YEAR)
        var date : String
        var status : String
        var dayString : String

        Log.i(TAG2, "InternID: $studId Year: $year Month: $month")

        statusArrayList.clear()
        entryTimeArrayList.clear()
        leaveTimeArrayList.clear()

        for (n in 2.. firstDayOfMonth) { //replacement for the giant when condition that has been commented out below
            statusArrayList.add("")
            entryTimeArrayList.add("")
            leaveTimeArrayList.add("")
        }


        path.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i(TAG2, "The calendar dates arraylist has ${customCalendarDatesArrayList.size} elements.")
                for (day in 1..customCalendarDatesArrayList.size) { //search range across a month
                    dayString = if (day < 10){
                        "0$day"
                    } else {
                        day.toString()
                    }
                    date = "$year-$month-$dayString" //"$chosenYear-$chosenMonth-$chosenDay"
                    if(snapshot.child("$date/Internship/$studId").exists()) {
                        Log.i(TAG2, "Attendance found on $date")
                        status = snapshot.child("$date/Internship/$studId/Status").value.toString()
                        if (status != "MC") {
                            statusArrayList.add(status)
                            entryTimeArrayList.add(snapshot.child("$date/Internship/$studId/EntryTime").value.toString())
                            leaveTimeArrayList.add(snapshot.child("$date/Internship/$studId/LeaveTime").value.toString())
                        }
                        else {
                            statusArrayList.add(status)
                            entryTimeArrayList.add("")
                            leaveTimeArrayList.add("")
                        }
                    }
                    else {
                        Log.i(TAG2, "Date record not found.")
                        statusArrayList.add("Absent")
                        entryTimeArrayList.add("")
                        leaveTimeArrayList.add("")
                    }
                }
                customCalendar.adapter?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG2, "Unable to query database.")
            }
        }  )
    }

    //adapter for custom calendar
    inner class CustomCalendarAdapter: RecyclerView.Adapter<CustomCalendarAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomCalendarAdapter.ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.calendar_date_card,parent,false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.dateCustom.text = customCalendarDatesArrayList[position]

            holder.calendarLayout.setBackgroundColor(Color.parseColor("#FFFFFF")) //background color becomes default white
            holder.dateCustom.setTextColor(Color.parseColor("#292929")) //text color becomes default

            when(statusArrayList[position]){
                "Present" -> {holder.calendarLayout.setBackgroundColor(Color.parseColor("#2ACC4C"))
                              holder.dateCustom.setTextColor(Color.parseColor("#FFFFFF"))} //changes to green for present status

                "Late" -> {holder.calendarLayout.setBackgroundColor(Color.parseColor("#CC1010"))
                           holder.dateCustom.setTextColor(Color.parseColor("#FFFFFF"))} //changes to red for late status

                "MC" -> {holder.calendarLayout.setBackgroundColor(Color.parseColor("#5A5A5A"))
                         holder.dateCustom.setTextColor(Color.parseColor("#FFFFFF"))} //changes to light gray for absent status

                "Absent" -> {holder.calendarLayout.setBackgroundColor(Color.parseColor("#FFFFFF"))
                       holder.dateCustom.setTextColor(Color.parseColor("#292929"))}//changes to default for no data/not included in the calendar list
            }
        }

        override fun getItemCount(): Int {
            return customCalendarDatesArrayList.size
        }

        inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
            var dateCustom: TextView = itemView.findViewById(R.id.customCalendarText)
            var calendarLayout: RelativeLayout = itemView.findViewById(R.id.backgroundLayout)

            init { //when pressed, would show the details layout.
                itemView.setOnClickListener {
                    val position = adapterPosition //gets the position of the selected array in int

                    binding.details.visibility = View.VISIBLE
                    binding.detailsDateText.text = "${customCalendarDatesArrayList[position]} ${changeMonthToString(chosenMonth)} $chosenYear"
                    binding.entryTimeDetailsText.text = entryTimeArrayList[position] //getEntryTime()
                    binding.leaveTimeDetailsText.text = leaveTimeArrayList[position] //getLeaveTime()
                }
            }
        }

    } //end of customCalendar adapter
} //426 lines nice