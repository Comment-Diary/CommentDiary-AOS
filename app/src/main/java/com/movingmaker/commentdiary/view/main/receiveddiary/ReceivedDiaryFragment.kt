package com.movingmaker.commentdiary.view.main.receiveddiary

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.movingmaker.commentdiary.CodaApplication
import com.movingmaker.commentdiary.R
import com.movingmaker.commentdiary.base.BaseFragment
import com.movingmaker.commentdiary.databinding.FragmentReceiveddiaryBinding
import com.movingmaker.commentdiary.global.CodaSnackBar
import com.movingmaker.commentdiary.model.entity.ReceivedDiary
import com.movingmaker.commentdiary.model.remote.RetrofitClient
import com.movingmaker.commentdiary.viewmodel.FragmentViewModel
import com.movingmaker.commentdiary.viewmodel.receiveddiary.ReceivedDiaryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ReceivedDiaryFragment : BaseFragment(), CoroutineScope {
    override val TAG: String = ReceivedDiaryFragment::class.java.simpleName

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    companion object {
        fun newInstance(): ReceivedDiaryFragment {
            return ReceivedDiaryFragment()
        }
    }

    private lateinit var binding: FragmentReceiveddiaryBinding
    private val fragmentViewModel: FragmentViewModel by activityViewModels()
    private val receivedDiaryViewModel: ReceivedDiaryViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentReceiveddiaryBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.receivedDiaryviewModel = receivedDiaryViewModel
        initViews()
        observeDatas()
        return binding.root

    }

    @SuppressLint("ResourceAsColor")
    private fun observeDatas() {

        fragmentViewModel.fragmentState.observe(viewLifecycleOwner) { fragment ->
            if (fragment == "receivedDiary") {
                //?????? ?????? ??????
                launch(coroutineContext) {
                    binding.loadingBar.isVisible = true
                    launch(Dispatchers.IO) {
                        receivedDiaryViewModel.setResponseGetReceivedDiary()
                    }
                }
            }
        }

        receivedDiaryViewModel.responseGetReceivedDiary.observe(viewLifecycleOwner) {
            binding.loadingBar.isVisible = false
            if (it.isSuccessful) {
                it.body()?.let { response ->
                    receivedDiaryViewModel.setReceivedDiary(response.result)
                    binding.commentLayout.isVisible = true
                    binding.diaryLayout.isVisible = true
                    binding.noReceivedDiaryYet.isVisible = false
                    //?????? ??? ???????????? ?????? ??????
//                    Log.d(TAG, "ob receivedDIarajiremioar ${response.result.myComment!!.isEmpty()}")
                    if (response.result.myComment?.isNotEmpty() == true) {
                        binding.sendCommentButton.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.background_ivory_radius_15_border_brown_1
                        )
                        binding.sendCommentButton.text = getString(R.string.diary_send_complete)
                        binding.sendCommentButton.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.text_brown
                            )
                        )
                        binding.commentLimitTextView.isVisible = false
                        binding.commentEditTextView.setText(response.result.myComment[0].content)
                        binding.commentEditTextView.isEnabled = false
                    } else {
                        binding.sendCommentButton.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.background_pure_green_radius_15
                        )
                        binding.commentLimitTextView.isVisible = true
                        binding.sendCommentButton.text = getString(R.string.send_text1)
                        binding.sendCommentButton.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.background_ivory
                            )
                        )
                        binding.commentEditTextView.text = null
                        binding.commentEditTextView.isEnabled = true

                    }
                }
            }
            //????????? ????????? ???????????? 404
            else {
                it.errorBody()?.let { errorBody ->
                    RetrofitClient.getErrorResponse(errorBody)?.let {
                        if (it.status == 401) {
//                            if(it.code=="EXPIRED_TOKEN")
                            //????????? ?????? ????????? ?????? ???????????? ???????????????
                            Toast.makeText(requireContext(), "?????? ???????????? ?????????.", Toast.LENGTH_SHORT)
                                .show()
                            CodaApplication.getInstance().logOut()
                        }
                    }
                }
                binding.commentLayout.isVisible = false
                binding.diaryLayout.isVisible = false
                binding.noReceivedDiaryYet.isVisible = true
            }
        }

        receivedDiaryViewModel.responseSaveComment.observe(viewLifecycleOwner) { response ->
            binding.loadingBar.isVisible = false
            if (response.isSuccessful) {
                CodaSnackBar.make(binding.root, "???????????? ?????????????????????.").show()
                //?????? ??????
                launch(coroutineContext) {
                    launch(Dispatchers.IO) {
                        receivedDiaryViewModel.setResponseGetReceivedDiary()
                    }
                }
            } else {
                Log.d(TAG, "observeDatas: ${response.errorBody()}")
                response.errorBody()?.let { errorBody ->
                    RetrofitClient.getErrorResponse(errorBody)?.let {
                        if (it.status == 401) {
                            Toast.makeText(requireContext(), "?????? ???????????? ?????????.", Toast.LENGTH_SHORT)
                                .show()
                            CodaApplication.getInstance().logOut()
                        } else {
                            CodaSnackBar.make(binding.root, "???????????? ???????????? ???????????????.").show()
                        }
                    }
                }
            }
        }

        receivedDiaryViewModel.responseReportDiary.observe(viewLifecycleOwner) { response ->
            binding.loadingBar.isVisible = false
            if (response.isSuccessful) {
                //?????? ??????
                launch(coroutineContext) {
                    launch(Dispatchers.IO) {
                        receivedDiaryViewModel.setResponseGetReceivedDiary()
                    }
                }
            } else {
                response.errorBody()?.let { errorBody ->
                    RetrofitClient.getErrorResponse(errorBody)?.let {
                        if (it.status == 401) {
                            Toast.makeText(requireContext(), "?????? ???????????? ?????????.", Toast.LENGTH_SHORT)
                                .show()
                            CodaApplication.getInstance().logOut()
                        } else {
                            CodaSnackBar.make(binding.root, "??????/????????? ???????????? ???????????????.").show()
                        }
                    }
                }
            }
        }

    }

    private fun initViews() = with(binding) {

        commentEditTextView.addTextChangedListener {
            receivedDiaryViewModel.setCommentTextCount(
                commentEditTextView.text.length
            )
        }

        sendCommentButton.setOnClickListener {
            showSendDialog()
        }

        reportButton.setOnClickListener {
            showReportDialog()
        }
        blockButton.setOnClickListener {
            showBlockDialog()
        }
    }

    private fun showSendDialog() {
        val dialogView = Dialog(requireContext())
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.setContentView(R.layout.dialog_receiveddiary_send_comment)
        dialogView.setCancelable(false)
        dialogView.show()


        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        submitButton.setOnClickListener {
            //????????? ??????
            launch(coroutineContext) {
                binding.loadingBar.isVisible = true
                launch(Dispatchers.IO) {
                    receivedDiaryViewModel.setResponseSaveComment(
                        binding.commentEditTextView.text.toString()
                    )
                }
                dialogView.dismiss()
            }
        }
        cancelButton.setOnClickListener {
            dialogView.dismiss()
        }

    }

    private fun showReportDialog() {
        val dialogView = Dialog(requireContext())
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.setContentView(R.layout.dialog_common_report)
        dialogView.setCancelable(false)
        dialogView.show()

        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val reportContentEditText = dialogView.findViewById<EditText>(R.id.reportContentEditText)

        submitButton.setOnClickListener {
            val reportContent = reportContentEditText.text.toString()
            if (reportContent.isEmpty()) {
                //?????? ?????? ??? ?????? ????????????~
                val shake: Animation = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                reportContentEditText.startAnimation(shake)
            }
            //??? ?????? ?????? ???????????????
            else {
                //??????
                launch(coroutineContext) {
                    binding.loadingBar.isVisible = true
                    launch(Dispatchers.IO) {
                        receivedDiaryViewModel.setResponseReportDiary(reportContent)
                    }
                }
                dialogView.dismiss()
            }
        }
        cancelButton.setOnClickListener {
            dialogView.dismiss()
        }
    }

    private fun showBlockDialog() {
        val dialogView = Dialog(requireContext())
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.setContentView(R.layout.dialog_common_block)
        dialogView.setCancelable(false)
        dialogView.show()

        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        submitButton.setOnClickListener {
            //?????? (?????? api??? ??? ????????? ??????)
            launch(coroutineContext) {
                binding.loadingBar.isVisible = true
                launch(Dispatchers.IO) {
                    receivedDiaryViewModel.setResponseReportDiary("")
                }
            }
            dialogView.dismiss()
        }
        cancelButton.setOnClickListener {
            dialogView.dismiss()
        }
    }

}