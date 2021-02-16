package com.ngeeann.myinternship20

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.login_groupset_ui.view.*

class LoginGroupSelect: DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView: View = inflater.inflate(R.layout.login_groupset_ui,container)
        val intent= Intent(activity,LoginSetup::class.java)

        rootView.cancelButton.setOnClickListener{
            dismiss()
        }
        rootView.internButton.setOnClickListener {
            intent.putExtra("userType", "Intern")
            startActivity(intent)
        }
        rootView.npisButton.setOnClickListener {
            intent.putExtra("userType","NPIS")
            startActivity(intent)
        }
        rootView.studentButton.setOnClickListener {
            intent.putExtra("userType","Student")
            startActivity(intent)
        }
        rootView.npStaffButton.setOnClickListener {
            intent.putExtra("userType","NP Staff")
            startActivity(intent)
        }

        return rootView
    }
}