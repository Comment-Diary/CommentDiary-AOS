package com.movingmaker.commentdiary.view.onboarding

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.movingmaker.commentdiary.CodaApplication
import com.movingmaker.commentdiary.R
import com.movingmaker.commentdiary.base.BaseActivity
import com.movingmaker.commentdiary.databinding.ActivityOnboardingLoginBinding
import com.movingmaker.commentdiary.global.CodaSnackBar
import com.movingmaker.commentdiary.model.remote.RetrofitClient
import com.movingmaker.commentdiary.view.main.MainActivity
import com.movingmaker.commentdiary.viewmodel.onboarding.OnboardingViewModel
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class OnboardingLoginActivity : BaseActivity<ActivityOnboardingLoginBinding>(), CoroutineScope {
    override val TAG: String = OnboardingLoginActivity::class.java.simpleName
    override val layoutRes: Int = R.layout.activity_onboarding_login

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val onboardingViewModel: OnboardingViewModel by viewModels()

    private lateinit var onboardingLoginFragment: OnboardingLoginFragment
    private lateinit var onboardingSignUpFragment: OnboardingSignUpFragment
    private lateinit var onboardingFindPasswordFragment: OnboardingFindPasswordFragment
    private lateinit var onboardingSignUpSuccessFragment: OnboardingSignUpSuccessFragment
    private var backButtonTime = 0L

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.statusBarColor = getColor(R.color.background_ivory)
        //todo ???????????? ?????? ??????
//        binding.backgroundLayout.setBackgroundColor(R.color.background_ivory)


        onboardingLoginFragment = OnboardingLoginFragment.newInstance()
        onboardingSignUpFragment = OnboardingSignUpFragment.newInstance()
        onboardingFindPasswordFragment = OnboardingFindPasswordFragment.newInstance()
        onboardingSignUpSuccessFragment = OnboardingSignUpSuccessFragment.newInstance()

        supportFragmentManager.beginTransaction()
            .add(binding.fragmentContainer.id, onboardingLoginFragment)
            .commit()

        binding.lifecycleOwner = this

        initViews()
        observeDatas()
    }

    private fun initViews() = with(binding) {
        addButtonEvent("login")
    }

    private fun observeDatas() {

        onboardingViewModel.responseFindPassword.observe(this) {
            binding.loadingBar.isVisible = false
            sendPasswordDialog(it.isSuccessful)
        }

        onboardingViewModel.responseLogin.observe(this) {
            binding.loadingBar.isVisible = false
            if (it.isSuccessful) {
                val accessToken = it.body()?.result?.accessToken
                val refreshToken = it.body()?.result?.refreshToken
                val accessTokenExpiresIn = it.body()?.result?.accessTokenExpiresIn

                if (accessToken == null || refreshToken == null || accessTokenExpiresIn == null) {
                } else {
                    CodaApplication.getInstance().getDataStore()
                        .insertAuth(accessToken, refreshToken, CodaApplication.customExpire)
                }

                startActivity(Intent(this, MainActivity::class.java).apply {
                    //?????? ???????????? ???????????? ?????? ?????? ?????? ????????? cleartask
                    //?????? ???????????? ????????? ??? Signin ??????
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
                it.errorBody()?.let{ errorBody->
                    val error = RetrofitClient.getErrorResponse(errorBody)
                    if(error!=null){
                        //?????? ???????????? ????????? ??????
                        if(error.status==403) {
                            CodaSnackBar.make(binding.root, error.message).show()
                            onboardingViewModel.setLoginCorrect(true)
                        }
                        else{
                            //????????? ?????? ????????? ?????? ?????????
                            onboardingViewModel.setLoginCorrect(false)
                        }
                    }
                    else{
                        //????????? ?????? ????????? ?????? ?????????
                        onboardingViewModel.setLoginCorrect(false)
                    }
                }
            }
        }

        onboardingViewModel.responseSignUpComplete.observe(this) {
            binding.loadingBar.isVisible = false
            if (it.isSuccessful) {
                CodaSnackBar.make(binding.root, "??????????????? ?????????????????????.").show()
                onboardingViewModel.setCurrentFragment("signUpSuccess")

            } else {
                CodaSnackBar.make(binding.root, "??????????????? ??????????????????.").show()
            }
        }

        val observeFragmentState = Observer<String> { fragment ->
            when (fragment) {
                "login" -> {
                    replaceFragment(onboardingLoginFragment)
                    binding.onboardingBottomButton.alpha = 1.0f
                    binding.onboardingBottomButton.isEnabled = true
                    binding.onboardingBottomButton.text = getString(R.string.onboarding_login)
                    addButtonEvent(fragment)
                }
                "signUp" -> {
                    replaceFragment(onboardingSignUpFragment)
                    binding.onboardingBottomButton.alpha = 0.4f
                    binding.onboardingBottomButton.isEnabled = false
                    binding.onboardingBottomButton.text = getString(R.string.onboarding_makeaccount)
                    addButtonEvent(fragment)
                    val observeButtonState = Observer<Boolean> { isCorrect ->
                        if (isCorrect) {
                            binding.onboardingBottomButton.alpha = 1.0f
                            binding.onboardingBottomButton.isEnabled = true
                        } else {
                            binding.onboardingBottomButton.alpha = 0.4f
                            binding.onboardingBottomButton.isEnabled = false
                        }
                    }
                    onboardingViewModel.canMakeAccount.observe(this, observeButtonState)
                }
                "findPW" -> {
                    replaceFragment(onboardingFindPasswordFragment)
                    binding.onboardingBottomButton.isEnabled = true
                    binding.onboardingBottomButton.text =
                        getString(R.string.onboarding_send_password)
                    addButtonEvent(fragment)
                }
                "signUpSuccess" -> {
                    replaceFragment(onboardingSignUpSuccessFragment)
                    binding.onboardingBottomButton.isEnabled = true
                    binding.onboardingBottomButton.text =
                        getString(R.string.onboarding_button_write_start)
                    addButtonEvent(fragment)
                }
            }
        }

        onboardingViewModel.currentFragment.observe(this, observeFragmentState)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(binding.fragmentContainer.id, fragment)
            commit()
        }
    }

    private fun addButtonEvent(fragment: String) {
        when (fragment) {
            "login" -> {
                binding.onboardingBottomButton.setOnClickListener {
                    if (onboardingViewModel.email.value!!.isEmpty() || onboardingViewModel.password.value!!.isEmpty()) {
                        onboardingViewModel.setLoginCorrect(false)
                    } else {
                        binding.loadingBar.isVisible = true
                        launch(coroutineContext) {
                            withContext(Dispatchers.IO) {
                                onboardingViewModel.setResponseLogin()
                            }
                        }
                    }
                }
            }
            "signUp" -> {
                binding.onboardingBottomButton.setOnClickListener {
                    if (onboardingViewModel.emailCodeCheckComplete.value == true) {
                        launch(coroutineContext) {
                            binding.loadingBar.isVisible = true
                            withContext(Dispatchers.IO) {
                                onboardingViewModel.setResponseSignUp()
                            }
                        }
                        return@setOnClickListener
                    } else {
                        CodaSnackBar.make(binding.root, "???????????? ????????? ?????????.").show()
                    }

                }
            }
            "findPW" -> {
                binding.onboardingBottomButton.setOnClickListener {
                    showPasswordNoticeDialog()
                }
            }
            "signUpSuccess" -> {
                binding.onboardingBottomButton.setOnClickListener {
                    binding.loadingBar.isVisible = true
                    launch(coroutineContext) {
                        withContext(Dispatchers.IO) {
                            onboardingViewModel.setResponseLogin()
                        }
                    }
                }
            }
        }
    }

    private fun showPasswordNoticeDialog() {
        val dialogView = Dialog(this)
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.setContentView(R.layout.dialog_onboarding_find_password)
        dialogView.setCancelable(false)
        dialogView.show()

        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        submitButton.setOnClickListener {
            launch(coroutineContext) {
                binding.loadingBar.isVisible = true
                withContext(Dispatchers.IO) {
                    onboardingViewModel.setResponseFindPassword()
                }
            }
            dialogView.dismiss()
        }
        cancelButton.setOnClickListener {
            dialogView.dismiss()
        }
    }

    private fun sendPasswordDialog(isSuccess: Boolean) {
        val dialogView = Dialog(this@OnboardingLoginActivity)
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.setContentView(R.layout.dialog_onboarding_password_send)
        dialogView.show()

        val closeButton = dialogView.findViewById<ImageButton>(R.id.closeButton)
        val okButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cannotFoundEmailTextView =
            dialogView.findViewById<TextView>(R.id.cannotFoundEmailTextView)
        val findPasswordSuccessTextView =
            dialogView.findViewById<TextView>(R.id.findPasswordSuccessTextView)

        if (isSuccess) {
            closeButton.isVisible = false
            okButton.isVisible = true
            cannotFoundEmailTextView.isVisible = false
            findPasswordSuccessTextView.isVisible = true
        } else {
            closeButton.isVisible = true
            okButton.isVisible = false
            cannotFoundEmailTextView.isVisible = true
            findPasswordSuccessTextView.isVisible = false
        }

        okButton.setOnClickListener {
            onboardingViewModel.setCurrentFragment("login")
            dialogView.dismiss()
        }
        closeButton.setOnClickListener {
            dialogView.dismiss()
        }
    }

    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        val gapTime = currentTime - backButtonTime
        val curFragment = onboardingViewModel.currentFragment.value
        if (curFragment != "signUp" && curFragment != "findPW") {
            if (gapTime in 0..2000) {
                // 2??? ?????? ?????? ???????????? ?????? ??? ??? ??????
                finishAndRemoveTask()
                android.os.Process.killProcess(android.os.Process.myPid())
            } else {
                backButtonTime = currentTime
                CodaSnackBar.make(binding.root, "???????????? ????????? ?????? ??? ????????? ???????????????.").show()
            }
        } else {
            onboardingViewModel.setCurrentFragment("login")
        }
    }
}


