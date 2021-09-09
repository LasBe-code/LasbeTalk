package com.example.lasbetalk

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.lasbetalk.model.Friend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_registration.*


private lateinit var auth: FirebaseAuth
lateinit var database: DatabaseReference

@Suppress("DEPRECATION")
class RegistrationActivity: AppCompatActivity() {
    private var imageUri : Uri? = null

    //이미지 등록
    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if(result.resultCode == RESULT_OK) {
                imageUri = result.data?.data //이미지 경로 원본
                registration_iv.setImageURI(imageUri) //이미지 뷰를 바꿈
                Log.d("이미지", "성공")
            }
            else{
                Log.d("이미지", "실패")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        auth = Firebase.auth
        database = Firebase.database.reference

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        val email = et_registration_id.text
        val password = et_registration_password.text
        val name = findViewById<EditText>(R.id.et_registration_name).text
        val button = findViewById<Button>(R.id.btn_registration)
        val profile = findViewById<ImageView>(R.id.registration_iv)
        var profileCheck = false

        profile.setOnClickListener{
            val intentImage = Intent(Intent.ACTION_PICK)
            intentImage.type = MediaStore.Images.Media.CONTENT_TYPE
            getContent.launch(intentImage)
            profileCheck = true
        }

        val intent = Intent(this, LoginActivity::class.java)

        button.setOnClickListener {
            if(email.isEmpty() && password.isEmpty() && name.isEmpty() && profileCheck)  {
                Toast.makeText(this, "아이디와 비밀번호, 프로필 사진을 제대로 입력해주세요.", Toast.LENGTH_SHORT).show()
                Log.d("Email", "$email, $password")
            }

            else{
                if(!profileCheck){
                    Toast.makeText(this, "프로필사진을 등록해주세요.", Toast.LENGTH_SHORT).show()
                } else{
                    auth.createUserWithEmailAndPassword(email.toString(), password.toString())
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val user = Firebase.auth.currentUser
                                val userId = user?.uid
                                val userIdSt = userId.toString()

                                FirebaseStorage.getInstance()
                                        .reference.child("userImages").child("$userIdSt/photo").putFile(imageUri!!).addOnSuccessListener {
                                            var userProfile: Uri? = null
                                            FirebaseStorage.getInstance().reference.child("userImages").child("$userIdSt/photo").downloadUrl
                                                    .addOnSuccessListener {
                                                        userProfile = it
                                                        Log.d("이미지 URL", "$userProfile")
                                                        val friend = Friend(email.toString(), name.toString(), userProfile.toString(), userIdSt)
                                                        database.child("users").child(userId.toString()).setValue(friend)
                                                    }
                                        }
                                Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                Log.e(TAG, "$userId")
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, "등록에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }
        }
    }
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if(currentUser != null){
            reload();
        }
    }

    private fun reload() {
    }

    companion object {
        private const val TAG = "EmailPassword"
    }
}