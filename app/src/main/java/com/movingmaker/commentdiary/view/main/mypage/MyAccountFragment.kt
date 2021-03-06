package com.movingmaker.commentdiary.view.main.mypage

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.movingmaker.commentdiary.CodaApplication
import com.movingmaker.commentdiary.base.BaseFragment
import com.movingmaker.commentdiary.databinding.FragmentMypageBinding
import com.movingmaker.commentdiary.databinding.FragmentMypageMyaccountBinding
import com.movingmaker.commentdiary.global.CodaSnackBar
import com.movingmaker.commentdiary.model.remote.RetrofitClient
import com.movingmaker.commentdiary.model.remote.request.ChangePasswordRequest
import com.movingmaker.commentdiary.viewmodel.FragmentViewModel
import com.movingmaker.commentdiary.viewmodel.mypage.MyPageViewModel
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MyAccountFragment: BaseFragment(), CoroutineScope {
    override val TAG: String = MyAccountFragment::class.java.simpleName

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val myPageViewModel: MyPageViewModel by activityViewModels()
    private val fragmentViewModel: FragmentViewModel by activityViewModels()

    companion object{
        fun newInstance() : MyAccountFragment {
            return MyAccountFragment()
        }
    }

    private lateinit var binding: FragmentMypageMyaccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentMypageMyaccountBinding.inflate(layoutInflater)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.lifecycleOwner = viewLifecycleOwner
        initViews()
        observeDatas()

        return binding.root
    }

    private fun observeDatas(){

        binding.lifecycleOwner?.let { lifecycleOwner ->
            myPageViewModel.responseLogOut.observe(lifecycleOwner) {
                binding.loadingBar.isVisible = false
                if (it.isSuccessful) {
                    logOut()
                } else {
                    it.errorBody()?.let{ errorBody->
                        RetrofitClient.getErrorResponse(errorBody)?.let{
                            if(it.status==404 || it.status==401){
                                Toast.makeText(requireContext(), "?????? ???????????? ?????????.", Toast.LENGTH_SHORT).show()
                                CodaApplication.getInstance().logOut()
                            }
                            else {
                                CodaSnackBar.make(binding.root, it.message).show()
                            }
                        }
                    }
                }

            }
        }

    }

    private fun initViews() = with(binding){
        logoutLayout.setOnClickListener {
            launch(coroutineContext) {
                loadingBar.isVisible = true
                //datastore ????????? ??????????????? background thread?????? ?????????????????? ?????? ??????????????? ??? ????????? IO
                withContext(Dispatchers.IO) {
                    myPageViewModel.setResponseLogOut()
                }
            }
        }
        signOutLayout.setOnClickListener {
            fragmentViewModel.setBeforeFragment("myAccount")
            fragmentViewModel.setFragmentState("signOut")
        }
        changePasswordLayout.setOnClickListener {
            fragmentViewModel.setBeforeFragment("myAccount")
            fragmentViewModel.setFragmentState("changePassword")
        }
        backButton.setOnClickListener {
            fragmentViewModel.setFragmentState("myPage")
        }
    }

    private fun logOut(){
        CodaApplication.getInstance().logOut()
    }

}