package com.movingmaker.commentdiary.view.main.receiveddiary

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.movingmaker.commentdiary.R
import com.movingmaker.commentdiary.global.base.BaseFragment
import com.movingmaker.commentdiary.databinding.FragmentReceiveddiaryBinding
import com.movingmaker.commentdiary.global.CodaSnackBar
import com.movingmaker.commentdiary.util.FRAGMENT_NAME
import com.movingmaker.commentdiary.viewmodel.FragmentViewModel
import com.movingmaker.commentdiary.viewmodel.receiveddiary.ReceivedDiaryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class ReceivedDiaryFragment : BaseFragment(), CoroutineScope {
    override val TAG: String = ReceivedDiaryFragment::class.java.simpleName

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

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
        binding.vm = receivedDiaryViewModel
        fragmentViewModel.setCurrentFragment(FRAGMENT_NAME.RECEIVED_DIARY)
        initViews()
        observeDatas()
        receivedDiaryViewModel.setResponseGetReceivedDiary()
        return binding.root

    }

    @SuppressLint("ResourceAsColor")
    private fun observeDatas() {

        receivedDiaryViewModel.receivedDiary.observe(viewLifecycleOwner) { receivedDiary ->
            //도착한 일기가 없는 경우
            if (receivedDiary == null) {
                binding.commentLayout.isVisible = false
                binding.diaryLayout.isVisible = false
                binding.noReceivedDiaryYet.isVisible = true
            }
            //도착한 일기가 있는 경우
            else {
                binding.commentLayout.isVisible = true
                binding.diaryLayout.isVisible = true
                binding.noReceivedDiaryYet.isVisible = false
                if (receivedDiary.myComment?.isNotEmpty() == true) {
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
                    binding.commentEditTextView.setText(receivedDiary.myComment[0].content)
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

        receivedDiaryViewModel.errorMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

//        receivedDiaryViewModel.toastMessage.observe(viewLifecycleOwner){
//            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
//        }

        receivedDiaryViewModel.snackMessage.observe(viewLifecycleOwner) {
            CodaSnackBar.make(binding.root, it).show()
        }

        receivedDiaryViewModel.loading.observe(viewLifecycleOwner) {
            binding.loadingBar.isVisible = it
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
            //코멘트 전송
            receivedDiaryViewModel.setResponseSaveComment(
                binding.commentEditTextView.text.toString()
            )
            dialogView.dismiss()
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
                //내용 입력 안 하면 흔들흔들~
                val shake: Animation = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                reportContentEditText.startAnimation(shake)
            }
            //한 글자 이상 입력했으면
            else {
                //신고
                receivedDiaryViewModel.setResponseReportDiary(reportContent)
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
            //차단 (신고 api에 빈 값으로 호출)
            receivedDiaryViewModel.setResponseReportDiary("")
            dialogView.dismiss()
        }
        cancelButton.setOnClickListener {
            dialogView.dismiss()
        }
    }

}