package com.movingmaker.commentdiary.view.main.mypage

import android.content.Context
import android.os.Bundle
import android.text.Layout
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.fragment.app.activityViewModels
import com.movingmaker.commentdiary.CodaApplication
import com.movingmaker.commentdiary.base.BaseFragment
import com.movingmaker.commentdiary.databinding.FragmentMypageBinding
import com.movingmaker.commentdiary.global.CodaSnackBar
import com.movingmaker.commentdiary.model.remote.RetrofitClient
import com.movingmaker.commentdiary.model.remote.request.ChangePasswordRequest
import com.movingmaker.commentdiary.viewmodel.FragmentViewModel
import com.movingmaker.commentdiary.viewmodel.mypage.MyPageViewModel
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MyPageFragment : BaseFragment(), CoroutineScope {
    override val TAG: String = MyPageFragment::class.java.simpleName

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val myPageViewModel: MyPageViewModel by activityViewModels()
    private val fragmentViewModel: FragmentViewModel by activityViewModels()

    private var temperatureBarMaxWidthPx = 0
//    private lateinit var display: Display

    companion object {
        fun newInstance(): MyPageFragment {
            return MyPageFragment()
        }
    }

    private lateinit var binding: FragmentMypageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentMypageBinding.inflate(layoutInflater)

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //????????? observer???
        binding.lifecycleOwner = viewLifecycleOwner
        binding.mypageviewmodel = myPageViewModel
        bindButtons()
        observeDatas()

        return binding.root
    }

    private fun observeDatas() {

        binding.lifecycleOwner?.let { lifecycleOwner ->
            fragmentViewModel.fragmentState.observe(lifecycleOwner) { fragment ->
                if (fragment == "myPage") {
                    launch(coroutineContext) {
                        binding.loadingBar.isVisible = true
                        withContext(Dispatchers.IO) {
                            myPageViewModel.setResponseGetMyPage()
                        }
                    }
                }
            }
        }
        binding.lifecycleOwner?.let { lifecycleOwner ->
            myPageViewModel.responseGetMyPage.observe(lifecycleOwner) { response ->
                binding.loadingBar.isVisible = false
                //?????? ????????? ???????????? ?????????
                if (response.isSuccessful) {
                    myPageViewModel.setMyAccount(response.body()!!.result.email)
                    myPageViewModel.setTemperature(response.body()!!.result.temperature)
                    myPageViewModel.setPushYN(response.body()!!.result.pushYN)
                    setTemperatureBar()
                }
                //?????? ????????? ???????????? ??????
                else {
                    response.errorBody()?.let{ errorBody->
                        RetrofitClient.getErrorResponse(errorBody)?.let{
                            if (it.status == 401) {
//                            if(it.code=="EXPIRED_TOKEN")
                                //????????? ?????? ????????? ?????? ???????????? ???????????????
                                Toast.makeText(requireContext(), "?????? ???????????? ?????????.", Toast.LENGTH_SHORT).show()
                                CodaApplication.getInstance().logOut()
                            }
                            else {
                                CodaSnackBar.make(binding.root,"??? ????????? ???????????? ??? ?????????????????????.").show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindButtons() = with(binding) {
        myAccountLayout.setOnClickListener {
            fragmentViewModel.setBeforeFragment("myPage")
            fragmentViewModel.setFragmentState("myAccount")
        }
        termsAndPolicyLayout.setOnClickListener {
            fragmentViewModel.setBeforeFragment("myPage")
            fragmentViewModel.setFragmentState("terms")
        }
        pushAlarmLayout.setOnClickListener {
            fragmentViewModel.setBeforeFragment("myPage")
            fragmentViewModel.setFragmentState("pushAlarmOnOff")
        }
        myCommentLayout.setOnClickListener {
            fragmentViewModel.setBeforeFragment("myPage")
            fragmentViewModel.setFragmentState("sendedCommentList")
        }
    }

    private fun setTemperatureBar() = with(binding) {
        //dp??? ?????????
//        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R){
//            display = requireActivity().display!!
//            display.getRealMetrics()
//        }
//        else{
//            display = requireActivity().windowManager.defaultDisplay
//        }
//        val displayMetrics = DisplayMetrics()
//        val dpi = displayMetrics.densityDpi
//        val density = displayMetrics.density
        //?????? ????????? 0.0??? ?????? 0dp??? ???????????? css,cee parent??? ?????? ????????? ?????? ?????? 1.0?????? ??????
        var temperature = myPageViewModel.temperature.value ?: 1.0
        if (temperature == 0.0) {
            temperature = 1.0
        }

        //?????? maxWidthPx
        if(temperatureBarMaxWidthPx==0){
            temperatureBarMaxWidthPx = temperatureBar.width
        }
        val temperatureRateForPx = (temperatureBarMaxWidthPx * (temperature / 100.0))
        //temperatureBar??? ?????? viewgroup?????? ?????? ?????? ??????
        val layoutParams: ConstraintLayout.LayoutParams =
            temperatureBar.layoutParams as ConstraintLayout.LayoutParams
        //6dp = 18px, ??????????????? ??????
        layoutParams.setMargins(17, 10, 17, 10)
        layoutParams.width = temperatureRateForPx.toInt()
        layoutParams.height = 17
        temperatureBar.layoutParams = layoutParams

        //invisible??? ??????????????? ?????? temperature ???????????? ??????, ?????? ?????? ??? ????????????
        temperatureBar.isVisible = true

//        Log.d(TAG, "setTemperatureBar: ${displayMetrics.densityDpi} ${displayMetrics.density}")
    }
}