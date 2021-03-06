package com.movingmaker.commentdiary.view.main.mydiary

import android.annotation.SuppressLint
import android.graphics.Point
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.core.view.marginTop
import androidx.fragment.app.activityViewModels
import com.movingmaker.commentdiary.CodaApplication
import com.movingmaker.commentdiary.R
import com.movingmaker.commentdiary.base.BaseFragment
import com.movingmaker.commentdiary.databinding.FragmentMydiaryWithCalendarBinding
import com.movingmaker.commentdiary.global.CodaSnackBar
import com.movingmaker.commentdiary.model.entity.Diary
import com.movingmaker.commentdiary.model.remote.RetrofitClient
import com.movingmaker.commentdiary.util.DateConverter
import com.movingmaker.commentdiary.util.Extension
import com.movingmaker.commentdiary.util.Extension.toDp
import com.movingmaker.commentdiary.util.Extension.toPx
import com.movingmaker.commentdiary.view.main.mydiary.calendardecorator.AloneDotDecorator
import com.movingmaker.commentdiary.view.main.mydiary.calendardecorator.CommentDotDecorator
import com.movingmaker.commentdiary.view.main.mydiary.calendardecorator.SelectedDateDecorator
import com.movingmaker.commentdiary.viewmodel.FragmentViewModel
import com.movingmaker.commentdiary.viewmodel.mydiary.MyDiaryViewModel
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

class CalendarWithDiaryFragment : BaseFragment(), CoroutineScope {
    override val TAG: String = CalendarWithDiaryFragment::class.java.simpleName

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var binding: FragmentMydiaryWithCalendarBinding

    private val myDiaryViewModel: MyDiaryViewModel by activityViewModels()
    private val fragmentViewModel: FragmentViewModel by activityViewModels()

    companion object {
        const val TAG: String = "??????"

        fun newInstance(): CalendarWithDiaryFragment {
            return CalendarWithDiaryFragment()
        }
//        var INSTANCE: CalendarWithDiaryFragment? = null
//        fun newInstance(): CalendarWithDiaryFragment {
//            if (INSTANCE == null) {
//                INSTANCE = CalendarWithDiaryFragment()
//            }
//            return INSTANCE!!
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMydiaryWithCalendarBinding.inflate(layoutInflater)

        //????????? ?????????????????? ?????? ??? ?????? ?????????????????? ??? ??????, commentdiarydetailfragment?????? ??? ?????? ???
        binding.lifecycleOwner = viewLifecycleOwner
//        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.myDiaryviewModel = myDiaryViewModel

        initSwipeRefresh()
        initViews()
        initButtons()
        observeData()
    }

    override fun onResume() {
        super.onResume()

    }

    private fun refreshViews() {
        Log.d(TAG, "observedatas refreshViews: ")
        //?????? ????????? ????????? ?????????, but ????????? ????????????
        val curDate = myDiaryViewModel.selectedDate.value
        //????????? ?????? ????????? ?????? ???
        if (curDate != null && curDate != "") {
            try {
                val dateYM = curDate.substring(0, 7)
                launch(coroutineContext) {
                    myDiaryViewModel.setResponseGetMonthDiary(dateYM)
                }
            } catch (e: Exception) {
            }
        }
    }


    private fun observeData() {

        myDiaryViewModel.pushDate.observe(viewLifecycleOwner) {
            Log.d(TAG, "observeData: pushpushpush ${myDiaryViewModel.pushDate.value}")
            val date = it
            val (y, m, d) = date.split('.').map { it.toInt() }
            checkSelectedDate(CalendarDay.from(y, m - 1, d))
            myDiaryViewModel.setSelectedDate(date)
            refreshViews()
            fragmentViewModel.setBeforeFragment("myDiary")
            fragmentViewModel.setFragmentState("commentDiaryDetail")
        }

        try {
        myDiaryViewModel.responseGetMonthDiary.observe(viewLifecycleOwner) {
            binding.loadingBar.isVisible = false
            if (it.isSuccessful) {
                    myDiaryViewModel.setMonthDiaries(it.body()!!.result)
                    //?????? ?????? ???????????? ???
                    if (myDiaryViewModel.selectedDate.value == null) {
                        checkSelectedDate(null)
                    } else {
                        val (y, m, d) = myDiaryViewModel.selectedDate.value!!.split('.')
                            .map { it.toInt() }
                        checkSelectedDate(CalendarDay.from(y, m - 1, d))
                    }
//                Toast.makeText(requireContext(), "????????? ??????", Toast.LENGTH_SHORT).show()
            } else {
                it.errorBody()?.let { errorBody ->
                    RetrofitClient.getErrorResponse(errorBody)?.let {
                        if (it.status == 401) {
//                            if(it.code=="EXPIRED_TOKEN")
                            //????????? ?????? ????????? ?????? ???????????? ???????????????
                            Toast.makeText(requireContext(), "?????? ???????????? ?????????.", Toast.LENGTH_SHORT).show()
                            CodaApplication.getInstance().logOut()
                        }
                    }
                }
            }
        }
        } catch (e: Exception) {
            Log.e(TAG, "observeData: ${e.stackTrace}")
        }
        myDiaryViewModel.monthDiaries.observe(viewLifecycleOwner) {
            binding.materialCalendarView.removeDecorators()
            binding.materialCalendarView.addDecorators(
                AloneDotDecorator(requireContext(), myDiaryViewModel.aloneDiary.value!!),
                CommentDotDecorator(requireContext(), myDiaryViewModel.commentDiary.value!!),
                SelectedDateDecorator(requireContext())
            )
        }

        fragmentViewModel.fragmentState.observe(viewLifecycleOwner) { fragment ->
            //?????? ???????????? ??? ??????!
            if (fragment == "myDiary") {
                refreshViews()
            }
        }
    }

    private fun initButtons() = with(binding) {
        //????????? ?????? ?????? writeDiary
        writeDiaryLayout.setOnClickListener {
            fragmentViewModel.setBeforeFragment("myDiary")
            fragmentViewModel.setFragmentState("writeDiary")
        }
        //????????? ?????? ??????
        readDiaryLayout.setOnClickListener {
            //???????????? ????????? writeDiary???, ?????? ????????? ????????? commentDiaryDetail
            //????????? ????????? ????????? ????????? ??????
            if (myDiaryViewModel.selectedDiary.value!!.tempYN == 'N' && myDiaryViewModel.selectedDiary.value!!.deliveryYN == 'Y') {
                fragmentViewModel.setBeforeFragment("myDiary")
                fragmentViewModel.setFragmentState("commentDiaryDetail")
            }
            //????????????(???????????????)??? ??????, ????????? ????????? ??????
            else {
                fragmentViewModel.setBeforeFragment("myDiary")
                fragmentViewModel.setFragmentState("writeDiary")
            }
        }
    }

    private fun initSwipeRefresh() = with(binding) {
        swipeRefreshLayout.setOnRefreshListener {
            val codaToday = DateConverter.ymdFormat(DateConverter.getCodaToday())
            //1??? ?????? ?????? ???, ?????? ?????????, ???????????? ???????????? ???
            //2??? ?????? ?????? ??? ?????????, ?????? ?????????
//            myDiaryViewModel.setSelectedDate(null)

            val (y, m, d) = codaToday.split('.').map { it.toInt() }
            materialCalendarView.currentDate = CalendarDay.from(y, m - 1, d)
            myDiaryViewModel.setSelectedDate(codaToday)
            checkSelectedDate(CalendarDay.from(y, m - 1, d))
//            checkSelectedDate(null)
            //3??? ?????? ?????? ????????? ?????????, ?????? ???????????????..
//            refreshViews()
            //?????? ?????? ????????? ???????????? ??????
            swipeRefreshLayout.isRefreshing = false
        }

        //???????????? ???????????? ?????????????????? ???????????? ??????????????? ??????????????? ??????????????? ??????
//        diaryContentsTextView.viewTreeObserver.addOnScrollChangedListener {
//            swipeRefreshLayout.isEnabled = diaryContentsTextView.scrollY==0
//        }

    }

    private fun initViews() = with(binding) {

        initCalendar()
    }

    @SuppressLint("SetTextI18n", "ResourceType")
    private fun initCalendar() = with(binding) {

        //????????? ??????????????? ????????? ?????????
        var display: Display
        val size = Point()
        Log.d(TAG, "initCalendar: width : ${size.x} ${size.y}")
//        Toast.makeText(requireContext(), "width: ${size.x} height : ${size.y}", Toast.LENGTH_LONG).show()
        if (size.x <= 1080) {
            val density: Float = calendarLine1.resources.displayMetrics.density

            materialCalendarView.apply {
                setTileSizeDp(40)
                setPadding(0, 0, 0, 30.toPx())
            }
            val layoutParams1: ConstraintLayout.LayoutParams =
                calendarLine1.layoutParams as ConstraintLayout.LayoutParams
            val layoutParams2: ConstraintLayout.LayoutParams =
                calendarLine2.layoutParams as ConstraintLayout.LayoutParams
            layoutParams1.setMargins(
                (calendarLine1.marginEnd.toDp() * density).roundToInt(),
                ((calendarLine1.marginTop.toDp() - 2) * density).roundToInt(),
                (calendarLine1.marginEnd.toDp() * density).roundToInt(),
                0
            )
            layoutParams2.setMargins(
                (calendarLine1.marginEnd.toDp() * density).roundToInt(),
                ((calendarLine2.marginTop.toDp() - 4) * density).roundToInt(),
                (calendarLine1.marginEnd.toDp() * density).roundToInt(),
                0
            )
//            Log.d(TAG, "initCalendar: ${calendarLine1.marginTop} ${calendarLine1.marginTop.toDp()}")
//            Log.d(TAG, "initCalendar: ${calendarLine2.marginTop} ${calendarLine2.marginTop.toDp()}")
//            Toast.makeText(requireContext(), "initCalendar: ${calendarLine1.marginTop} ${calendarLine1.marginTop.toDp()}\n initCalendar: ${calendarLine2.marginTop} ${calendarLine2.marginTop.toDp()}", Toast.LENGTH_SHORT).show()
//            val size : Int=  (30 * density).roundToInt()
//            float scale = view.getResources().getDisplayMetrics().density;

//            Log.d(TAG, "initCalendar: ${calendarLine1.marginEnd} ${}")
        }
//        val width = size.x
//        val height = size.y

        //nexus 720 1280
        //pixel2 1080 1920
        //?????????s21 1080 2400
        //note9 1440 2960
        //?????????, ?????? ???????????? ???
        materialCalendarView.setOnDateChangedListener { widget, date, selected ->
            checkSelectedDate(date)
        }

        //?????????, ??? ???????????? ???
        materialCalendarView.setOnMonthChangedListener { widget, date ->
            Log.d(TAG, "observedata setOnMonth: ")
            binding.calendarHeaderTextView.text = "${date.year}??? ${date.month + 1}???"
            //????????? ?????? ????????????
            myDiaryViewModel.setSelectedDiary(Diary(null, "", "", "", ' ', ' ', null))
            val requestDate = LocalDate.of(date.year, date.month + 1, date.day)
                .format(DateTimeFormatter.ofPattern("yyyy.MM"))
            setMonthCalendarDiaries(requestDate)
        }

        materialCalendarView.state().edit()
            .setFirstDayOfWeek(Calendar.SUNDAY)
            .setMinimumDate(CalendarDay.from(2021, 0, 1))//????????? ?????? ??????
            .setMaximumDate(CalendarDay.from(2022, 11, 31))//????????? ??? ??????
            .setCalendarDisplayMode(CalendarMode.MONTHS) // ??? ??????, ??? ??????
            .commit()
        materialCalendarView.setWeekDayFormatter(ArrayWeekDayFormatter(resources.getStringArray(R.array.custom_weekdays)))
//        materialCalendarView.setTitleFormatter(MonthArrayTitleFormatter(resources.getStringArray(R.array.custom_months)))
//        materialCalendarView.setHeaderTextAppearance(R.style.CalendarViewHeaderCustomText);
//        materialCalendarView.setWeekDayTextAppearance(R.drawable.background_ivory_radius_15_border_brown_1)
//        materialCalendarView.setHeaderTextAppearance(R.drawable.background_ivory_radius_15_border_brown_1)
        //topbar(??????)
//        materialCalendarView.setTitleFormatter { day -> // CalendarDay?????? ???????????? LocalDate ???????????? ???????????? ???????????? ????????????
//            // ???, ????????? ????????? ????????? ?????? (MonthArrayTitleFormatter??? ????????? ??????????????? ?????? setTitleFormatter()??? ?????????)
//            val calendarHeaderBuilder = StringBuilder()
//            calendarHeaderBuilder.append(day.year)
//                .append("??? ")
//                .append(day.month + 1)
//                .append("???")
//            calendarHeaderBuilder.toString()
//        }
//        materialCalendarView.setWeekDayTextAppearance()
        materialCalendarView.addDecorator(
            SelectedDateDecorator(requireContext())
        )
        materialCalendarView.isDynamicHeightEnabled = true

        materialCalendarView.topbarVisible = false
        leftArrowButton.setOnClickListener {
            val beforeDate = materialCalendarView.currentDate
            materialCalendarView.currentDate =
                CalendarDay.from(beforeDate.year, beforeDate.month - 1, beforeDate.day)
        }

        rightArrowButton.setOnClickListener {
            val beforeDate = materialCalendarView.currentDate
            materialCalendarView.currentDate =
                CalendarDay.from(beforeDate.year, beforeDate.month + 1, beforeDate.day)
        }

    }


    private fun setMonthCalendarDiaries(yearMonth: String) {
        //?????? ???????????? ?????? ????????? ??????
        if (myDiaryViewModel.selectedDate.value == "") {
            myDiaryViewModel.setSelectedDate(DateConverter.ymdFormat(DateConverter.getCodaToday()))
        }
        //??? ?????? ??? ????????? x
        else {
            myDiaryViewModel.setSelectedDate(null)
        }
//        binding.materialCalendarView.selectedDate = null
        //??? ?????? ??? ?????? ?????? ?????? ??????

        launch(coroutineContext) {
            binding.loadingBar.isVisible = true
            withContext(Dispatchers.IO) {
                myDiaryViewModel.setResponseGetMonthDiary(yearMonth)
            }
        }
    }


//    private fun checkSelectedDate(date: CalendarDay?) = with(binding){
//        //        Log.d(TAG, "checkSelectedDate: date ${date}")
//        //calendar date??? month??? 0?????? ???????????? ????????? ?????? ?????? localDate??? ?????? ??? +1
//        if(date!=null) {
//
//            var selectedDate = LocalDate.of(date.year, date.month + 1, date.day)
//            val dateToString = selectedDate.toString().replace('-', '.')
//            var nextDate = LocalDate.of(date.year, date.month + 1, date.day)
//            Log.d(TAG, "observeDatas: detail 0 ${myDiaryViewModel.selectedDate.value}: ")
//            nextDate = nextDate.plusDays(1)
//
//            val nextDateToString = nextDate.toString().replace('-', '.')
//
//            myDiaryViewModel.setSelectedDate(dateToString)
//
////        Log.d(TAG, "checkSelectedDate: ??????  ?????? $nextDateToString")
////        Log.d(TAG, "checkSelectedDate: ?????? ?????? ${dateToString}")
//            //?????? ?????? ???????????? ?????? ?????? ?????? ????????? ???????????? ??? ?????? ?????? ???????????? ??? ???????????? ?????? -> Day+1
//            launch(Dispatchers.IO) {
//                myDiaryViewModel.setResponseGetDayComment(nextDateToString)
//            }
//        }
//    }


    @SuppressLint("ResourceAsColor")
    fun checkSelectedDate(date: CalendarDay?) = with(binding) {
        Log.d(TAG, "checkSelectedDate: push ${myDiaryViewModel.pushDate.value}")
//        materialCalendarView.currentDate = date
        materialCalendarView.selectedDate = date
        //??? ????????? ?????? ????????? ??????
        if (date == null) {
            readDiaryLayout.isVisible = false
            writeDiaryWrapLayout.isVisible = false
            noCommentTextView.isVisible = false
            return
        }
        //?????? ????????? ????????? ??????????????? ????????? ??????

        //calendar date??? month??? 0?????? ???????????? ????????? ?????? ?????? localDate??? ?????? ??? +1
        var selectedDate = LocalDate.of(date.year, date.month + 1, date.day)
        val dateToString = selectedDate.toString().replace('-', '.')
        var nextDate = LocalDate.of(date.year, date.month + 1, date.day)
        nextDate = nextDate.plusDays(1)

        val nextDateToString = nextDate.toString().replace('-', '.')


        myDiaryViewModel.setSelectedDate(dateToString)


//        Log.d(TAG, "checkSelectedDate: ??????  ?????? $nextDateToString")
//        Log.d(TAG, "checkSelectedDate: ?????? ?????? ${dateToString}")
        //?????? ?????? ???????????? ?????? ?????? ?????? ????????? ???????????? ??? ?????? ?????? ???????????? ??? ???????????? ?????? -> Day+1
        launch(Dispatchers.IO) {
            myDiaryViewModel.setResponseGetDayComment(nextDateToString)
        }

        val codaToday = DateConverter.getCodaToday()
//        Log.d(TAG, "checkSelectedDate: ?????? $codaToday $selectedDate")

        //?????? ????????? ?????? ?????? ??????, ?????? ??????
        if (selectedDate > codaToday) {
            writeDiaryWrapLayout.isVisible = true
            writeDiaryLayout.isVisible = false
            lineDecorationTextView.text = getString(R.string.write_diary_yet)
            readDiaryLayout.isVisible = false
            noCommentTextView.isVisible = false
            return
        }
        //?????? ????????? databinding?????? ?????????
        myDiaryViewModel.setDateDiaryText(
            "${String.format("%02d", date.month + 1)}??? ${String.format("%02d", date.day)}??? ?????? ??????"
        )
//        Log.d(TAG, "chekcselectedDate selectedDiary.value.deliverYN: ${myDiaryViewModel.selectedDiary.value!!.deliveryYN}")

        //?????? ????????? ??????
        if (myDiaryViewModel.monthDiaries.value?.isNotEmpty() == true) {
            for (diary in myDiaryViewModel.monthDiaries.value!!) {
                //????????? ????????? ?????????
                if (diary.date == selectedDate.toString().replace('-', '.')) {
                    //viewmodel??? ?????????
                    myDiaryViewModel.setSelectedDiary(diary)

                    //?????? ????????? ??????
                    if (diary.deliveryYN == 'N') {
                        sendDiaryBeforeAfterTextView.isVisible = false
                        noCommentTextView.isVisible = false
                    }
                    //????????? ????????? ??????
                    else {
                        sendDiaryBeforeAfterTextView.isVisible = true
                        //????????????????????? ????????? ????????? ????????? ?????????
                        //???????????? ?????? ???????????? ?????? ?????? or ????????? ?????? or???????????? ??? ??? ????????? ??? null?

                        if (diary.commentList == null || diary.commentList.size == 0) {
                            //???????????? ???????????? ???????????? ?????? ?????? ??????
                            if (DateConverter.ymdToDate(diary.date) <= codaToday.minusDays(2)) {
                                sendDiaryBeforeAfterTextView.isVisible = false
                                noCommentTextView.isVisible = diary.tempYN == 'N'
                            }
                            //?????? ????????? ???????????? ??????
                            else {
                                //??????????????? ??????
                                noCommentTextView.isVisible = false
                                if (diary.tempYN == 'Y') {
                                    sendDiaryBeforeAfterTextView.setTextColor(
                                        ContextCompat.getColor(
                                            requireContext(),
                                            R.color.text_black
                                        )
                                    )
                                    sendDiaryBeforeAfterTextView.text =
                                        getString(R.string.upload_temp_comment_please)
                                    sendDiaryBeforeAfterTextView.setBackgroundResource(R.drawable.background_brand_orange_radius_bottom_10)
                                } else {
                                    sendDiaryBeforeAfterTextView.setTextColor(
                                        ContextCompat.getColor(
                                            requireContext(),
                                            R.color.text_brown
                                        )
                                    )
                                    sendDiaryBeforeAfterTextView.text =
                                        getString(R.string.calendar_with_diary_comment_soon)
                                    sendDiaryBeforeAfterTextView.setBackgroundResource(R.drawable.background_light_brown_radius_bottom_10)
                                }
                            }
                        }
                        //????????? ?????? ??????
                        else {
                            val codaToday = DateConverter.getCodaToday()
                            //????????? ???????????? ????????? ?????? ??? ???  ????????? ??? ?????????
                            binding.sendDiaryBeforeAfterTextView.isVisible = true
//                                DateConverter.ymdToDate(diary.date) > codaToday.minusDays(2)

//                            if(DateConverter.ymdToDate(diary.date)<=codaToday.minusDays(2) && !myDiaryViewModel.haveDayMyComment.value!!){
//                                binding.sendDiaryBeforeAfterTextView.isVisible = false
//                            }
//                            else{
//                                binding.sendDiaryBeforeAfterTextView.isVisible = true
//                            }
                            sendDiaryBeforeAfterTextView.text = getString(R.string.arrived_comment)
                            sendDiaryBeforeAfterTextView.setBackgroundResource(R.drawable.background_pure_green_radius_bottom_10)
                            sendDiaryBeforeAfterTextView.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.background_ivory
                                )
                            )
                            noCommentTextView.isVisible = false
                        }
                    }
                    readDiaryLayout.isVisible = true
                    writeDiaryWrapLayout.isVisible = false
                    return
                }
            }
        }

        //????????? ?????????
        myDiaryViewModel.setSelectedDiary(
            Diary(
                null,
                "",
                "",
                selectedDate.toString().replace('-', '.'),
                ' ',
                ' ',
                null
            )
        )
        writeDiaryWrapLayout.isVisible = true
        writeDiaryLayout.isVisible = true
        lineDecorationTextView.text = getString(R.string.mydiary_text_decoration)
        readDiaryLayout.isVisible = false
        noCommentTextView.isVisible = false
    }

}