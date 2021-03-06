package com.movingmaker.commentdiary.view.main.mypage

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.movingmaker.commentdiary.CodaApplication
import com.movingmaker.commentdiary.R
import com.movingmaker.commentdiary.base.BaseFragment
import com.movingmaker.commentdiary.databinding.FragmentMypageBinding
import com.movingmaker.commentdiary.databinding.FragmentMypageMyaccountBinding
import com.movingmaker.commentdiary.databinding.FragmentMypageSendedCommentListBinding
import com.movingmaker.commentdiary.global.CodaSnackBar
import com.movingmaker.commentdiary.model.remote.RetrofitClient
import com.movingmaker.commentdiary.model.remote.request.ChangePasswordRequest
import com.movingmaker.commentdiary.util.DateConverter
import com.movingmaker.commentdiary.view.main.gatherdiary.DiaryListFragment
import com.movingmaker.commentdiary.viewmodel.FragmentViewModel
import com.movingmaker.commentdiary.viewmodel.mypage.MyPageViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class SendedCommentListFragment : BaseFragment(), CoroutineScope {
    override val TAG: String = SendedCommentListFragment::class.java.simpleName

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val myPageViewModel: MyPageViewModel by activityViewModels()
    private val fragmentViewModel: FragmentViewModel by activityViewModels()

    private var searchPeriod="all"

    companion object {
        private const val MAX_YEAR = 2099
        private const val MIN_YEAR = 1980

        fun newInstance(): SendedCommentListFragment {
            return SendedCommentListFragment()
        }
    }

    private lateinit var binding: FragmentMypageSendedCommentListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentMypageSendedCommentListBinding.inflate(layoutInflater)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.mypageviewmodel= myPageViewModel
        observeDatas()
        initViews()
        return binding.root
    }

    private fun observeDatas() {

        fragmentViewModel.fragmentState.observe(viewLifecycleOwner){ fragment->
            if(fragment=="sendedCommentList"){
                setComments()
            }
        }

        myPageViewModel.responseGetAllComment.observe(viewLifecycleOwner) {
            binding.loadingBar.isVisible = false
            if (it.isSuccessful) {
                it.body()?.result?.let { commentList -> myPageViewModel.setCommentList(commentList) }
            } else {
                it.errorBody()?.let{ errorBody->
                    RetrofitClient.getErrorResponse(errorBody)?.let{
                        if(it.status==401){
                            Toast.makeText(requireContext(), "?????? ???????????? ?????????.", Toast.LENGTH_SHORT).show()
                            CodaApplication.getInstance().logOut()
                        }
                        else {
                            CodaSnackBar.make(binding.root, "???????????? ???????????? ??? ??????????????????.")
                        }
                    }
                }
            }
        }
        myPageViewModel.responseGetMonthComment.observe(viewLifecycleOwner) {
            binding.loadingBar.isVisible = false
            if (it.isSuccessful) {
                it.body()?.result?.let { commentList -> myPageViewModel.setCommentList(commentList) }
            } else {
                it.errorBody()?.let{ errorBody->
                    RetrofitClient.getErrorResponse(errorBody)?.let{
                        if(it.status==404 || it.status==401){
                            Toast.makeText(requireContext(), "?????? ???????????? ?????????.", Toast.LENGTH_SHORT).show()
                            CodaApplication.getInstance().logOut()
                        }
                        else {
                            CodaSnackBar.make(binding.root, "???????????? ???????????? ??? ??????????????????.")
                        }
                    }
                }
            }
        }
    }

    private fun initViews() = with(binding) {
        backButton.setOnClickListener {
            fragmentViewModel.setFragmentState("myPage")
        }
        selectDateLayout.setOnClickListener {
            showDialog()
        }
    }

    private fun setComments() {
        launch(coroutineContext) {
            binding.loadingBar.isVisible = true
            launch(Dispatchers.IO) {
                when (searchPeriod) {
                    "all" -> {
                        myPageViewModel.setResponseGetAllComment()
                    }
                    else -> {
                        myPageViewModel.setResponseGetMonthComment(searchPeriod)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showDialog() {

        val dialogView = Dialog(requireContext())
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.setContentView(R.layout.dialog_common_select_date)

        dialogView.show()

        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)
        val allPeriodButton = dialogView.findViewById<Button>(R.id.allPeriodButton)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.yearPicker)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.monthPicker)

        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        yearPicker.minValue = MIN_YEAR
        yearPicker.maxValue = MAX_YEAR
        val (y,m) = myPageViewModel.selectedMonth.value!!.split('.')
        yearPicker.value = y.toInt()
        monthPicker.value = m.toInt()

        val typeface = resources.getFont(R.font.robotomedium)
        setNumberPickerStyle(yearPicker,
            ContextCompat.getColor(requireContext(), R.color.text_black), typeface)
        setNumberPickerStyle(monthPicker,
            ContextCompat.getColor(requireContext(), R.color.text_black), typeface)

        saveButton.setOnClickListener {
            // ????????? ?????? ???????????? ??????
            val date = "${yearPicker.value}.${String.format("%02d",monthPicker.value)}"
            searchPeriod = date
            setComments()
            myPageViewModel.setSelectedMonth(date)
            binding.selectDateTextView.text = "${yearPicker.value}??? ${String.format("%02d",monthPicker.value)}???"
            dialogView.dismiss()
        }

        allPeriodButton.setOnClickListener {
            // ?????? ??????
            searchPeriod = "all"
            setComments()
            myPageViewModel.setSelectedMonth(DateConverter.ymFormat(DateConverter.getCodaToday()))
            binding.selectDateTextView.text = getString(R.string.show_all)
            dialogView.dismiss()
        }

    }

    // ?????? ?????? ????????? ?????? ???????????? ??????
    @SuppressLint("DiscouragedPrivateApi")
    private fun setNumberPickerStyle(numberPicker: NumberPicker, color: Int, typeface: Typeface) {
        //?????? ??????????????? ???????????? ?????????
        numberPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val count = numberPicker.childCount
            for (i in 0..count) {
                val child = numberPicker.getChildAt(i)
                if (child is TextView) {
                    try {
                        child.setTextColor(color)
//                        child.typeface = typeface
                        numberPicker.invalidate()
//                        Log.d(TAG, "setNumberPickerText: downversion ${child::class.java.simpleName}")
                        var selectorWheelPaintField =
                            numberPicker.javaClass.getDeclaredField("mSelectorWheelPaint")
                        var accessible = selectorWheelPaintField.isAccessible
                        selectorWheelPaintField.isAccessible = true
                        (selectorWheelPaintField.get(numberPicker) as Paint).color = color
                        selectorWheelPaintField.isAccessible = accessible
                        (selectorWheelPaintField.get(numberPicker) as Paint).typeface = typeface

                        numberPicker.invalidate()
                        var selectionDividerField =
                            numberPicker.javaClass.getDeclaredField("mSelectionDivider")
                        accessible = selectionDividerField.isAccessible
                        selectionDividerField.isAccessible = true
                        selectionDividerField.set(numberPicker, null)
                        selectionDividerField.isAccessible = accessible
                        (selectionDividerField.get(numberPicker) as Paint).typeface = typeface
                        numberPicker.invalidate()
                    } catch (exception: Exception) {
                    }
                }
            }
        } else {
//            Log.d(TAG, "setNumberPickerStyle: upversion")
            numberPicker.textColor = color
            val count = numberPicker.childCount
            for (i in 0..count) {
                val child = numberPicker.getChildAt(i)
                try {
                    if(child is TextView) {
//                        child.typeface = typeface
                        val paint = Paint()
                        paint.typeface = typeface
                        numberPicker.setLayerPaint(paint)
                        numberPicker.invalidate()
                    }
                } catch (exception: Exception) {
                }

                numberPicker.invalidate()
            }
        }
    }
}

