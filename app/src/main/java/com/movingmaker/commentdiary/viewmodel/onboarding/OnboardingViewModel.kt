package com.movingmaker.commentdiary.viewmodel.onboarding

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.movingmaker.commentdiary.CodaApplication
import com.movingmaker.commentdiary.model.remote.request.EmailCodeCheckRequest
import com.movingmaker.commentdiary.model.remote.request.LogInRequest
import com.movingmaker.commentdiary.model.remote.request.SignUpRequest
import com.movingmaker.commentdiary.model.remote.response.IsSuccessResponse
import com.movingmaker.commentdiary.model.remote.response.LogInResponse
import com.movingmaker.commentdiary.model.repository.ForSignUpRespository
import kotlinx.coroutines.withContext
import retrofit2.Response

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private var _emailCorrect = MutableLiveData<Boolean>()
    private var _passwordCorrect = MutableLiveData<Boolean>()
    private var _passwordCheckCorrect = MutableLiveData<Boolean>()
    private var _canMakeAccount = MutableLiveData<Boolean>()
    private var _currentFragment = MutableLiveData<String>()
    private var _emailCodeCheckComplete = MutableLiveData<Boolean>()
    private var _loginCorrect = MutableLiveData<Boolean>()
    //api response
    private var _responseEmailSend = MutableLiveData<Response<IsSuccessResponse>>()
    private var _responseEmailCodeCheck = MutableLiveData<Response<IsSuccessResponse>>()
    private var _responseSignUpComplete = MutableLiveData<Response<IsSuccessResponse>>()
    private var _responseLogin = MutableLiveData<Response<LogInResponse>>()
    private var _responseFindPassword = MutableLiveData<Response<IsSuccessResponse>>()

    //for signUp, logIn data share
    private var _email = MutableLiveData<String>()
    private var _password = MutableLiveData<String>()
    private var _checkPassword = MutableLiveData<String>()

    //for findPassword
    private var _findPasswordEmail = MutableLiveData<String>()


    val emailCorrect: LiveData<Boolean>
        get() = _emailCorrect

    val passwordCorrect: LiveData<Boolean>
        get() = _passwordCorrect

    val passwordCheckCorrect: LiveData<Boolean>
        get() = _passwordCheckCorrect

    val canMakeAccount: LiveData<Boolean>
        get() = _canMakeAccount

    val currentFragment: LiveData<String>
        get() = _currentFragment

    val responseEmailSend: LiveData<Response<IsSuccessResponse>>
        get() = _responseEmailSend

    val responseIsSuccessCheck: LiveData<Response<IsSuccessResponse>>
        get() = _responseEmailCodeCheck

    val emailCodeCheckComplete: LiveData<Boolean>
        get() = _emailCodeCheckComplete

    val responseSignUpComplete: LiveData<Response<IsSuccessResponse>>
        get() = _responseSignUpComplete

    val email: LiveData<String>
        get() = _email

    val password: LiveData<String>
        get() = _password

    val checkPassword: LiveData<String>
        get() = _checkPassword

    val responseLogin: LiveData<Response<LogInResponse>>
        get() = _responseLogin

    val loginCorrect: LiveData<Boolean>
        get() = _loginCorrect

    val findPasswordEmail: LiveData<String>
        get() = _findPasswordEmail

    val responseFindPassword: LiveData<Response<IsSuccessResponse>>
        get() = _responseFindPassword


    init {
        _emailCorrect.value = true
        _passwordCorrect.value = true
        _passwordCheckCorrect.value = true
        _canMakeAccount.value = false
        _currentFragment.value = ""
        _emailCodeCheckComplete.value = false
        _email.value = ""
        _password.value = ""
        _checkPassword.value = ""
        _loginCorrect.value = true
        _findPasswordEmail.value =""
    }

    fun setSignUpIsCorrect(isCorrect: Boolean, type: String) {
        when (type) {
            "email" -> {
                _emailCorrect.value = isCorrect
            }
            "password" -> {
                _passwordCorrect.value = isCorrect
            }
            "passwordCheck" -> {
                _passwordCheckCorrect.value = isCorrect
            }
        }
    }

    fun setCanMakeAccount(isCanMakeAccount: Boolean) {
        _canMakeAccount.value =
            emailCorrect.value!! && passwordCorrect.value!! && passwordCheckCorrect.value!! && isCanMakeAccount
    }

    fun setCurrentFragment(fragment: String) {
        _currentFragment.value = fragment
    }

    fun setEmailCodeCheckComplete(isComplete: Boolean) {
        _emailCodeCheckComplete.value = isComplete
    }

    fun setEmail(text: String){
        _email.value = text
    }

    fun setPassword(text: String){
        _password.value = text
    }

    fun setCheckPassword(text: String){
        _checkPassword.value = text
    }

    fun setLoginCorrect(isCorrect: Boolean){
        _loginCorrect.value = isCorrect
    }

    fun setFindPasswordEmail(text: String){
        _findPasswordEmail.value = text
    }


    suspend fun setResponseEmailSend(email: String) {
            withContext(viewModelScope.coroutineContext) {
                _responseEmailSend.value =
                    ForSignUpRespository.INSTANCE.sendEmailCode(email = email)
            }
    }

    suspend fun setResponseEmailCodeCheck(email: String, code: Int) {
        withContext(viewModelScope.coroutineContext) {
            _responseEmailCodeCheck.value = ForSignUpRespository.INSTANCE.emailCodeCheck(
                EmailCodeCheckRequest(email, code)
            )
        }
    }

    suspend fun setResponseSignUp() {
        withContext(viewModelScope.coroutineContext) {
            _responseSignUpComplete.value = ForSignUpRespository.INSTANCE.signUp(
                SignUpRequest(
                    email = email.value!!,
                    password = password.value!!,
                    checkPassword = checkPassword.value!!
                )
            )
        }
    }

    suspend fun setResponseLogin(){
        withContext(viewModelScope.coroutineContext){
//            Log.d(TAG, "setResponseLogin: ${CodaApplication.deviceToken}")
            _responseLogin.value = ForSignUpRespository.INSTANCE.logIn(
                LogInRequest(
                    email = email.value!!,
                    password = password.value!!,
                    CodaApplication.deviceToken
                )
            )
        }
    }

    suspend fun setResponseFindPassword(){
        withContext(viewModelScope.coroutineContext){
            _responseFindPassword.value = ForSignUpRespository.INSTANCE.findPassword(findPasswordEmail.value!!)
        }
    }

}