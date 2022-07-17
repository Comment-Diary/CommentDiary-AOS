package com.movingmaker.commentdiary.view.onboarding

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.movingmaker.commentdiary.R
import com.movingmaker.commentdiary.databinding.FragmentOnboardingLoginBeforeBinding
import com.movingmaker.commentdiary.util.FRAGMENT_NAME
import com.movingmaker.commentdiary.view.main.MainActivity
import com.movingmaker.commentdiary.view.main.mydiary.CalendarWithDiaryFragment.Companion.TAG
import com.movingmaker.commentdiary.viewmodel.onboarding.OnboardingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.log

class OnboardingLoginBeforeFragment : Fragment(), CoroutineScope {

    private lateinit var binding: FragmentOnboardingLoginBeforeBinding

    private val onboardingViewModel: OnboardingViewModel by activityViewModels()

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentOnboardingLoginBeforeBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        onboardingViewModel.setCurrentFragment(FRAGMENT_NAME.LOGIN_BEFORE)
        initViews()
        return binding.root
    }

    private fun initViews() {
        binding.startWithEmailButton.setOnClickListener {
            findNavController().navigate(OnboardingLoginBeforeFragmentDirections.actionOnboardingLoginBeforeFragmentToOnboardingLoginFragment())
        }

        binding.startWithKakaoButton.setOnClickListener {
            // 카카오계정으로 로그인 공통 callback 구성
            // 카카오톡으로 로그인 할 수 없어 카카오계정으로 로그인할 경우 사용됨
            val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                if (error != null) {
                    Log.e(TAG, "카카오계정으로 로그인 실패", error)
                } else if (token != null) {
                    Log.i(TAG, "카카오계정으로 로그인 성공 ${token.accessToken}")
                    login(token.accessToken)
                }
            }

            // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(requireContext())) {
                UserApiClient.instance.loginWithKakaoTalk(requireContext()) { token, error ->
                    if (error != null) {
                        Log.e(TAG, "카카오톡으로 로그인 실패", error)
                        //todo 예외처리
                        Log.e(
                            TAG,
                            "${error is ClientError && error.reason == ClientErrorCause.Cancelled}",
                            error
                        )

                        // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                        // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            return@loginWithKakaoTalk
                        }

                        // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                        UserApiClient.instance.loginWithKakaoAccount(
                            requireContext(),
                            callback = callback
                        )
                    } else if (token != null) {
                        Log.i(TAG, "카카오톡으로 로그인 성공 ${token.accessToken}")
                        login(token.accessToken)
                    }
                }
            } else {
                UserApiClient.instance.loginWithKakaoAccount(requireContext(), callback = callback)
            }
        }
    }

    private fun login(kakaoToken: String) {
        launch {
            onboardingViewModel.onLoading()
            val (isSuccessLogin, isNewMember) = onboardingViewModel.kakaoLogin(kakaoToken)
            if (isSuccessLogin) {
                when (isNewMember) {
                    true -> {
                        findNavController().navigate(OnboardingLoginBeforeFragmentDirections.actionOnboardingLoginBeforeFragmentToOnboardingKakaoTermsFragment())
                    }
                    else -> {
                        startActivity(
                            Intent(
                                requireContext(),
                                MainActivity::class.java
                            ).apply {
                                requireActivity().finish()
                            })
                    }
                }
            }
        }
    }
}