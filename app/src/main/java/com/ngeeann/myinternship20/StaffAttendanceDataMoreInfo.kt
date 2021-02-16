package com.ngeeann.myinternship20

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.ngeeann.myinternship20.databinding.StaffAttendancedataMoreinfoBinding
import kotlinx.android.synthetic.main.staff_attendancedata_card.*
import kotlinx.android.synthetic.main.staff_attendancedata_moreinfo.*
import kotlinx.android.synthetic.main.ui_intern_main.*
import java.util.*
/*
1. Receives specified module, group, lesson time slot and day e.g. CCADC, LB12, 10:00 - 12:00 & Tuesday
2. Displays module, group, lesson time slot in their respective textviews
3. Displayed date will be the latest occurrence of that day (Tuesday) relative to current time e.g. If
    current day is Monday, display the previous week's Tuesday entries. If current day is Wednesday, display the current week's Tuesday Date and entry
4. Queries database with date, module-group value "attendance/$selectedDate/$modGroup and initializing studentNameArray
5. Entry display works by attempting to initialize 3 arrays with the values of Students based on their status. If no students in that array are found, display "No students in this list"
    5.1. OrderByChild "Name" value
    5.2. Checks what is their status, enters their current nameArray position into one of the arrays e.g. Students 1,2,4,5 are present and Student 3 is late, presentArray = {1,2,4,5}, lateArray = {3}
    5.3 Listing values in each tab will be done as nameArray[presentArray[n]]
6. When the forward or backward buttons are pressed, selectedDate is +/- 7 days and queried again
 */
@RequiresApi(Build.VERSION_CODES.O)
class StaffAttendanceDataMoreInfo : AppCompatActivity()/*, DatePickerDialog.OnDateSetListener*/ { //todo Make the total counter update when the query is completed

    private lateinit var binding: StaffAttendancedataMoreinfoBinding //using binding again
    private val database = Firebase.database.reference
    private val cal = Calendar.getInstance() //calendar instance for the date
    private val TAG = "StaffAttendanceDataMoreInfo Activity"

    var selectedIndexList = arrayListOf<Int>() //these 4 lists store reference points for the arraylists based on the status of a student
    var presentIndexList = arrayListOf<Int>()
    var lateIndexList = arrayListOf<Int>()
    var absIndexList = arrayListOf<Int>()

    var studentNameArrayList = arrayListOf<String>()
    var studentIdArrayList = arrayListOf<String>()
    var studentDetail = arrayListOf<String>()

    var chosenDate = 0 //chosen day
    var chosenMonth = 0 //chosen month in int
    var chosenYear = 0 //chosen year
    var status = "Present" //to set the default screen as "present" on navbar and keep the UI at the kept screen when changed date. "Present", "Late" and "Absent"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StaffAttendancedataMoreinfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val chosenModule = intent.getStringExtra("chosenModule").toString()
        val chosenGroup = intent.getStringExtra("chosenGroup").toString()
        val chosenStudentTotal = intent.getStringExtra("chosenStudentTotal").toString()
        val chosenDayOfWeek = intent.getStringExtra("chosenDay").toString()
        val chosenTiming = intent.getStringExtra("chosenTime").toString() //gets chosen time slot from previous activity

        binding.staffRecyclerView.layoutManager = LinearLayoutManager(this)//makes the recycler view a vertical scrollable list.

        binding.dateNextButton.visibility = View.GONE
        navBarSet()//set to the default upon opening

        val defaultDateString = revertDateCalendar(changeDayToInt(chosenDayOfWeek)) //called to get the correct date based on schedule
        binding.dateChosenText.text = "$chosenDate ${changeMonthToString(chosenMonth)} $chosenYear" //sets date into the textview
        queryLesson(chosenModule,chosenGroup,chosenStudentTotal)
        selectedIndexList = presentIndexList
        binding.totalDataTextView.text = "Total: ${selectedIndexList.size} / $chosenStudentTotal"

        binding.dateNextButton.setOnClickListener { //next date onclick
            nextDay(defaultDateString) //to get calendar to next date
            navBarSet() //to override data upon change of date
            Log.i(TAG,"Calendar date advanced by 7 days.")
            queryLesson(chosenModule, chosenGroup,chosenStudentTotal)
            binding.totalDataTextView.text = "Total: ${selectedIndexList.size} / $chosenStudentTotal"
        }

        binding.datePreviousButton.setOnClickListener { //previous date onclick
            binding.totalDataTextView.text = "Total: ${selectedIndexList.size} / $chosenStudentTotal"
            previousDay() //to get calendar to previous date
            navBarSet() //to override data upon change of date
            Log.i(TAG,"Calendar date returned by 7 days.")
            queryLesson(chosenModule, chosenGroup,chosenStudentTotal)
            binding.totalDataTextView.text = "Total: ${selectedIndexList.size} / $chosenStudentTotal"
        }

        binding.dateChosenText.setOnClickListener { //picks the date from an open calendar onclick
            binding.totalDataTextView.text = "Total: ${selectedIndexList.size} / $chosenStudentTotal"
            pickDate() //to get calendar to previous date
            navBarSet() //to override data upon change of date
        }

        binding.moduleView.text = chosenModule //puts the module value in the moduleView text view
        binding.groupView.text = chosenGroup //puts the group value in the groupView text view
        binding.classTimeView.text = chosenTiming //puts the timing of the class into the classTimeView text view
        binding.dayOfWeekTextView.text = chosenDayOfWeek//puts the day of the week into the dayOfWeekTextView text view

        binding.staffStudentDataBottomNav.setOnNavigationItemSelectedListener { item -> //navigation between "present", "late" and "absent"
            when (item.itemId) {
                //present data displayed
                R.id.nav_present -> {
                    status = "Present" //changes the status so it would go to present
                    selectedIndexList = presentIndexList
                    navBarSet()
                    binding.totalDataTextView.text = "Total: ${selectedIndexList.size} / $chosenStudentTotal"
                    //queryLesson(chosenModule, chosenGroup, status)
                }
                //late data displayed
                R.id.nav_late -> {
                    status = "Late" //changes to status so it would go to late
                    selectedIndexList = lateIndexList
                    navBarSet()
                    binding.totalDataTextView.text = "Total: ${selectedIndexList.size} / $chosenStudentTotal"
                    //queryLesson(chosenModule, chosenGroup, status)
                }
                //absent data displayed
                R.id.nav_absent -> {
                    status = "Absent" //changes to status so it would go to absent.
                    selectedIndexList = absIndexList
                    navBarSet()
                    binding.totalDataTextView.text = "Total: ${selectedIndexList.size} / $chosenStudentTotal"
                    //queryLesson(chosenModule, chosenGroup, "MC")
                }
            }
            true
        }//end of navigation between "Present", "Late" and "Absent"

        binding.staffStudentDataBackArrow2.setOnClickListener { //ends the activity with the back arrow. goes back to the overview.
            this.finish()
        }
    }

    private fun navBarSet(){
        staffRecyclerView.adapter = DataRecyclerAdapter() //puts data in respective item slot in the recycler view
    }

    private fun getDateCalendar(){
        chosenDate = cal.get(Calendar.DATE)
        chosenMonth = cal.get(Calendar.MONTH)
        chosenYear = cal.get(Calendar.YEAR)
    }

    private fun revertDateCalendar(dd:Int):String{
        if(dd > cal.get(Calendar.DAY_OF_WEEK)){
            cal.set(Calendar.DAY_OF_WEEK,dd)
            cal.add(Calendar.DATE,-7)
        }
        else{
            cal.set(Calendar.DAY_OF_WEEK,dd)
        }

        getDateCalendar()
        return "$chosenDate ${changeMonthToString(chosenMonth)} $chosenYear"
    }

    private fun pickDate(){
        getDateCalendar()
        // DatePickerDialog(this,this,chosenYear,chosenMonth,chosenDay).show()
    }

    private fun changeDayToInt(dd: String): Int {
        when(dd){
            "Sunday" -> return 1
            "Monday" -> return 2
            "Tuesday" -> return 3
            "Wednesday" -> return 4
            "Thursday" -> return 5
            "Friday" -> return 6
            "Saturday" -> return 7
        }
        return 0
    }

    private fun changeMonthToString(mm: Int): String{
        val monthArray = arrayOf("January","February","March","April","May","June",
            "July", "August","September","October","November","December")
        return monthArray[mm]
    }

    private fun nextDay(default:String){  //chooses next day
        cal.add(Calendar.DATE,7)
        getDateCalendar()
        binding.dateChosenText.text = "$chosenDate ${changeMonthToString(chosenMonth)} $chosenYear"
        if (binding.dateChosenText.text == default){
            binding.dateNextButton.visibility = View.GONE
        }
        else{
            binding.dateNextButton.visibility = View.VISIBLE
        }
    }

    private fun previousDay(){ //chooses previous day
        cal.add(Calendar.DATE,-7)
        getDateCalendar()
        binding.dateChosenText.text = "$chosenDate ${changeMonthToString(chosenMonth)} $chosenYear"
        binding.dateNextButton.visibility = View.VISIBLE
    }

    private fun conDateToString(): String{ //Format of YYYY-MM-DD
        val month = if ((chosenMonth + 1) < 10){
            "0${chosenMonth + 1}"
        }
        else {
            "${chosenMonth + 1}"
        }
        val date = if (chosenDate < 10){
            "0$chosenDate"
        }
        else {
            chosenDate
        }
        return "$chosenYear-$month-$date"
    }

    //adapter to display data of students
    inner class DataRecyclerAdapter: RecyclerView.Adapter<DataRecyclerAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataRecyclerAdapter.ViewHolder { //makes the items in recyclerview fill with the StaffStudentDataCard. Each item will be the card instead of "item 1" and so on. Ignore this. This is for layout purposes only.
            val v = LayoutInflater.from(parent.context).inflate(R.layout.staff_attendancedata_card,parent,false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.studentName.text = studentNameArrayList[selectedIndexList[position]] //displays the Student Name in respective text view from array position into respective item slot (like a for loop system)
            holder.studentID.text = studentIdArrayList[selectedIndexList[position]] //displays the Student ID in respective text view from array position into respective item slot
            holder.studentStatus.text = studentDetail[selectedIndexList[position]] //displays the Time they logged in into respective text view from array position into respective item slot

            //if the ui is showing absent, then it would show if the student has an MC or not
            if (status == "absent"){
                holder.studentStatus.text = "MC: ${studentDetail[selectedIndexList[position]]}"
            }
        }

        override fun getItemCount(): Int {
            return selectedIndexList.size //studentNameArrayList.size
        }

        //initializing of what values will be placed in which text view. Ignore this
        inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
            var studentName: TextView = itemView.findViewById(R.id.studentNameText_card)
            var studentID: TextView = itemView.findViewById(R.id.studentIdText_card)
            var studentStatus: TextView = itemView.findViewById(R.id.timeText_card)

            //when pressed, will bring up a toast for details
            init {
                itemView.setOnClickListener {
                    val position = adapterPosition //gets the position of the selected array in int
                    when {
                        studentDetail[position] == "Yes" -> { //it should be MC Link not timeStatus
                            Toast.makeText(itemView.context, "${studentNameArrayList[position]} has an Mc. It may open in another activity.", Toast.LENGTH_SHORT).show()
                        } //if the mc status is "yes", can open mc in another activity
                        studentDetail[position] == "No" -> {
                            Toast.makeText((itemView.context),"${studentNameArrayList[position]} does not have an Mc.",Toast.LENGTH_SHORT).show()
                        }//if the mc status is no.
                        else -> {
                            Toast.makeText(itemView.context, "${studentNameArrayList[position]} entered class at ${studentDetail[position]}.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun queryLesson(module: String, group: String, chosenStudentTotal:String) { //initializes 4 arrays with values from the lesson entry
        val date = conDateToString()
        val path = database.child("attendance/$date/$module-$group").orderByChild("Name")
        path.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    Log.i(TAG, "Records found, snapshot name ${snapshot.key.toString()}")

                    clearAttendanceLists() //its too long to put here so I made a little method that clears all 6 lists

                    for ((n,lessonSnapshot) in snapshot.children.withIndex()){
                        val status = lessonSnapshot.child("Status").getValue<String>().toString()
                        studentNameArrayList.add(lessonSnapshot.child("Name").getValue<String>().toString())
                        studentIdArrayList.add(lessonSnapshot.key.toString())
                        Log.d(TAG, "Student $n: ${studentNameArrayList[n]}")

                        if(lessonSnapshot.child("EntryTime").exists()) { //checks if student is on MC or not as present students have an entry time while sick ones have an MC link in their entry
                            studentDetail.add(lessonSnapshot.child("EntryTime").getValue<String>().toString())
                        }
                        else {
                            studentDetail.add(lessonSnapshot.child("MC Link").getValue<String>().toString())
                        }

                        when (status) {
                            "Present" -> {
                                presentIndexList.add(n)
                            }
                            "Late" -> {
                                lateIndexList.add(n)
                            }
                            else -> {
                                absIndexList.add(n)
                            }
                        }
                    }
                    staffRecyclerView.adapter = DataRecyclerAdapter()
                    binding.totalDataTextView.text = "Total: ${selectedIndexList.size} / $chosenStudentTotal"
                    staffRecyclerView.adapter?.notifyDataSetChanged()
                }
                else {
                    clearAttendanceLists() //clears 6 different lists, I'm sorry system memory :(
                    staffRecyclerView.adapter?.notifyDataSetChanged()
                    Toast.makeText(this@StaffAttendanceDataMoreInfo, "No lesson records on this date found.", Toast.LENGTH_SHORT).show()
                    binding.totalDataTextView.text = "Total: 0 / $chosenStudentTotal"
                    Log.w(TAG, "No records found, snapshot name ${snapshot.key.toString()}")
                    Log.d(TAG, "Date is $date")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@StaffAttendanceDataMoreInfo, "Failed to query, please try again.", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Query failure")
            }
        })
    }

    private fun clearAttendanceLists() {
        studentNameArrayList.clear()
        studentIdArrayList.clear()
        studentDetail.clear()
        presentIndexList.clear()
        lateIndexList.clear()
        absIndexList.clear()
    }
}

