package com.ngeeann.myinternship20

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.ngeeann.myinternship20.databinding.StaffAttendancedatahomeBinding
import kotlinx.android.synthetic.main.staff_attendancedatahome.*
import kotlinx.android.synthetic.main.staff_attendancedatahome.view.*
import kotlinx.android.synthetic.main.staff_attendanceoverview_card.*
/*
1. Receives array of modules that the staff is in charge of
2. Populates module array adapter with names of modules to select from
3. Launches query of module groups when loaded, display groups in the box below
4. When a different module is selected, run step 3 again
5. When user presses group, start MoreInfo activity and send module, group, timing and day info to activity
 */

class StaffAttendanceDataHome : AppCompatActivity() {

    private lateinit var binding: StaffAttendancedatahomeBinding//used binding again
    private val database = Firebase.database.reference
    val TAG = "StaffAttendHome"
    private var groupArrayList = arrayListOf("Group 1","Group 2")//test array for groups to see layout
    private var studentTotalArrayList = arrayListOf("Number 1", "Number 2")
    private var dayArrayList = arrayListOf("Tuesday", "Wednesday")//test array for day of the week to see the layout
    private var timeArrayList = arrayListOf("TS 1","TS 2")//test array for timing to see the layout
    private var chosenModule = "Module 1"//string to get chosen module

    override fun onCreate(savedInstanceState: Bundle?) {
        val userId = intent.getStringExtra("userId")
        val moduleArray = intent.getStringArrayListExtra("moduleArray")
            ?: arrayListOf("Module 1", "Module 2", "Module 3")
        super.onCreate(savedInstanceState)
        binding = StaffAttendancedatahomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.overviewRecyclerView.layoutManager = LinearLayoutManager(this)//makes the recycler view a vertical scrollable list.
        binding.overviewRecyclerView.adapter = OverviewRecyclerAdapter()//attached the adapter class for the recycler view

        groupQuery(userId.toString(), chosenModule) //checks for groups for selected module

        binding.staffStudentDataBackArrow.setOnClickListener {
            this.finish()
        }

        binding.staffModuleSpinner.adapter = ArrayAdapter(this,R.layout.custom_spinner_style, moduleArray)

        binding.staffModuleSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                chosenModule = moduleArray[position]
                groupQuery(userId.toString(), chosenModule)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        } //initialize chosenModule with the selected item in the array for Module drop box
    }

    //adapter to display data of students
    inner class OverviewRecyclerAdapter: RecyclerView.Adapter<OverviewRecyclerAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverviewRecyclerAdapter.ViewHolder {
            //makes the items in recyclerview fill with the StaffStudentDataCard. Each item will be the card instead of "item 1" and so on. Ignore this. This is for layout purposes only.
            val v = LayoutInflater.from(parent.context).inflate(R.layout.staff_attendanceoverview_card,parent,false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.groupTextView.text = groupArrayList[position] //displays the group into the designated text view
            holder.studentTotalTextView.text = "${studentTotalArrayList[position]} students" //displays the total number of students into the designated text view
            holder.dayTextView.text = dayArrayList[position] //displays the day of the week into the designated text view
            holder.timingTextView.text = timeArrayList[position] //displays the timing of the class into the designated text view
        }

        override fun getItemCount(): Int {
            return groupArrayList.size //this is to make sure the recycler view will display the all items in the array
        }

        //initializing of what values will be placed in which text view. Ignore this
        inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
            var groupTextView: TextView = itemView.findViewById(R.id.groupTextView)
            var studentTotalTextView: TextView = itemView.findViewById(R.id.studentTotalTextView)
            var dayTextView: TextView = itemView.findViewById(R.id.dayTextView)
            var timingTextView: TextView = itemView.findViewById(R.id.timingTextView)

            init { //when pressed, will open the activity with correct info
                itemView.setOnClickListener {
                    val position = adapterPosition //gets the position of the selected array in int

                    //start the next activity and transfers the data over
                    startActivity(Intent(this@StaffAttendanceDataHome,StaffAttendanceDataMoreInfo::class.java)
                            //extra data to transfer to the next activity
                            .putExtra("chosenModule",chosenModule)
                            .putExtra("chosenGroup",groupArrayList[position])
                            .putExtra("chosenStudentTotal",studentTotalArrayList[position])
                            .putExtra("chosenDay",dayArrayList[position])
                            .putExtra("chosenTime",timeArrayList[position]))
                }
            }
        }
    }

    private fun groupQuery(userId: String, module: String) {
        val path = database.child("users/$userId/Modules/$module")
        path.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                groupArrayList.clear()
                studentTotalArrayList.clear()
                dayArrayList.clear()
                timeArrayList.clear()

                for (grpSnapshot in snapshot.children){
                    groupArrayList.add(grpSnapshot.key.toString())
                    studentTotalArrayList.add(grpSnapshot.child("Total Students").value.toString())
                    dayArrayList.add(grpSnapshot.child("Day").value.toString())
                    timeArrayList.add(grpSnapshot.child("Time").value.toString())
                }
                overviewRecyclerView.adapter?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "Query Failure")
            }
        })
    }
}