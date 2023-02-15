package com.example.realtimelocation

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import io.paperdb.Paper
import com.example.realtimelocation.Utils.Common
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.util.Arrays
import com.example.realtimelocation.Model.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity() {
    companion object{
        private val MY_REQUEST_CODE = 7117 //any number
    }
    lateinit var user_information:DatabaseReference
    lateinit var providers:List<AuthUI.IdpConfig>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        init
        Paper.init(this)

//        Init firebase
        user_information = FirebaseDatabase.getInstance().getReference(Common.USER_INFORMATION)
//        Init providers
        providers = Arrays.asList<AuthUI.IdpConfig>(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
//        Request permission
        Dexter.withActivity(this).withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION).withListener(object:PermissionListener{
            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                showSignInOptions()
            }

            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                Toast.makeText(this@MainActivity,"You must accept this permission",Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: PermissionRequest?,
                p1: PermissionToken?
            ) {
                TODO("Not yet implemented")
            }

        }).check()
    }

    private fun showSignInOptions() {
        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build(),
            MY_REQUEST_CODE)
    }
    //        Crtl+O
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_REQUEST_CODE){
            val firebaseUser = FirebaseAuth.getInstance().currentUser
//            check if user exist on the db
            user_information.orderByKey().equalTo(firebaseUser!!.uid).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.value == null){
//                            User not exist
                            if (!dataSnapshot.child(firebaseUser!!.uid).exists()){
                                Common.loggedUser = com.example.realtimelocation.Model.User(
                                    firebaseUser.uid,
                                    firebaseUser.email!!
                                )
//                                Add user to db
                                user_information.child(Common.loggedUser.uid!!).setValue(Common.loggedUser)
                            }
                        }
                        else{
//                            User available
                            Common.loggedUser = dataSnapshot.child(firebaseUser.uid).getValue(User::class.java)!!
                        }
//                        Save UID to storage to update location from killed mode
                        Paper.book().write(Common.USER_UID_SAVE_KEY, Common.loggedUser.uid!!)
                        updateToken(firebaseUser)
                        setupUI()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
        }
    }

    private fun setupUI() {
        startActivity(Intent(this@MainActivity,HomeActivity::class.java))
        finish()
    }

    private fun updateToken(firebaseUser: FirebaseUser?) {
        val tokens = FirebaseDatabase.getInstance().getReference(Common.TOKENS);
//        Get Token
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
//            val msg = getString(R.string.msg_token_fmt, token)
            Log.d(TAG, token)
            Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
        })
    }
}


