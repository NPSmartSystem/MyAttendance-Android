package com.ngeeann.myinternship20

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.ngeeann.myinternship20.databinding.SetupUiBinding
import kotlinx.android.synthetic.main.setup_ui.*

class LoginSetup : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = SetupUiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getStringExtra("userType").toString()
        val arrayAdapterSchool = ArrayAdapter.createFromResource(applicationContext,R.array.school_list,android.R.layout.simple_spinner_item)

        binding.schoolSpinner.adapter = arrayAdapterSchool
        binding.setupUserType.text = "$type setup"
        binding.setupUserEmail.hint = "$type E-mail"
        binding.setupUserID.hint = "$type ID (Used for login)"
        binding.setupName.hint = "$type's Name (according to IC)"

        if(type == "NPIS" || type == "NP Staff"){
            textView8.visibility = View.GONE
            courseSpinner.visibility = View.GONE
        }

        if(type == "Intern"){
            setupIntern.visibility = View.VISIBLE
        }

        binding.schoolSpinner.onItemSelectedListener = object:AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                if(position == 0) {
                    binding.courseSpinner.adapter = ArrayAdapter.createFromResource(applicationContext,R.array.baCourse_list,android.R.layout.simple_spinner_item)
                }
                if(position == 1) {
                    binding.courseSpinner.adapter = ArrayAdapter.createFromResource(applicationContext,R.array.deCourse_List,android.R.layout.simple_spinner_item)
                }
                if(position == 2) {
                    val courseAdapterArray = ArrayAdapter.createFromResource(applicationContext,R.array.soeCourse_list,android.R.layout.simple_spinner_item)
                    binding.courseSpinner.adapter = courseAdapterArray
                }
                if(position == 3){
                    binding.courseSpinner.adapter = ArrayAdapter.createFromResource(applicationContext,R.array.fmsCourse_list,android.R.layout.simple_spinner_item)
                }
                if(position == 4 ) {
                    binding.courseSpinner.adapter = ArrayAdapter.createFromResource(applicationContext,R.array.hsCourse_list,android.R.layout.simple_spinner_item)
                }
                if(position == 5) {
                    binding.courseSpinner.adapter= ArrayAdapter.createFromResource(applicationContext,R.array.hssCourse_list,android.R.layout.simple_spinner_item)
                }
                if(position==6){
                    val arrayAdapter_course=ArrayAdapter.createFromResource(applicationContext,R.array.ictCourse_List,android.R.layout.simple_spinner_item)
                    binding.courseSpinner.adapter = arrayAdapter_course
                }
                if(position==7){
                    val arrayAdapter_course=ArrayAdapter.createFromResource(applicationContext,R.array.lsctCourse_list,android.R.layout.simple_spinner_item)
                    binding.courseSpinner.adapter=arrayAdapter_course
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.registerButton.setOnClickListener {
            if(binding.setupUserEmail.text.toString() == "" || binding.setupUserID.text.toString() == "" ||binding.setupPassword.text.toString()==""){
                Toast.makeText(this,"Fill in empty blanks",Toast.LENGTH_SHORT).show()
            }
            else{//successful creation
                startActivity(Intent(this,LoginMainScreen::class.java))//successful registration
                Toast.makeText(this,"Successfully created $type User",Toast.LENGTH_SHORT).show()
            }
        }

        binding.setupCancelButton.setOnClickListener{
            finish()
        }

    }
}