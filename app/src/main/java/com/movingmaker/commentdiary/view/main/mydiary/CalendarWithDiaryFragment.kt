package com.movingmaker.commentdiary.view.main.mydiary

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.movingmaker.commentdiary.CodaApplication
import com.movingmaker.commentdiary.R
import com.movingmaker.commentdiary.databinding.FragmentWritediaryCalendarWithDiaryBinding
import com.movingmaker.commentdiary.model.entity.Diary
import com.movingmaker.commentdiary.view.main.MainActivity
import com.movingmaker.commentdiary.view.main.mydiary.calendardecorator.AloneDotDecorator
import com.movingmaker.commentdiary.view.main.mydiary.calendardecorator.CommentDotDecorator
import com.movingmaker.commentdiary.viewmodel.mydiary.CalendarViewModel
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import kotlinx.coroutines.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class CalendarWithDiaryFragment: Fragment(R.layout.fragment_writediary_calendar_with_diary), CoroutineScope {

    //당일 날짜 디폴트 선택(회색 동글)
    //코멘트용 일기 레드 닷, 혼자 쓴 일기 블루 닷
    private val diaryList = arrayOf(
        Diary(
            id = 1,
            title = "제목1",
            content = "코멘트용 일기1",
            date = "2022.02.25",
            deliveryYN ="y",
            commentList = null
        ),
        Diary(
            id = 2,
            title = "제목2",
            content = "개인 일기1",
            date = "2022.02.28",
            deliveryYN ="n",
            commentList = null
        ),
        Diary(
            id = 3,
            title = "제목3",
            content = "코멘트용 일기2",
            date = "2022.02.4",
            deliveryYN ="n",
            commentList = null
        ),
        Diary(
            id = 4,
            title = "제목4",
            content = "개인 일기2",
            date = "2022.02.15",
            deliveryYN ="n",
            commentList = null
        ),
        Diary(
            id = 5,
            title = "제목5",
            content = "코멘트용 일기3",
            date = "2022.02.11",
            deliveryYN ="y",
            commentList = null
        ),
        Diary(
            id = 6,
            title = "제목6",
            content = "코멘트용 일기4",
            date = "2022.02.9",
            deliveryYN ="y",
            commentList = null
        ),
        Diary(
            id = 7,
            title = "제목7",
            content = "개인 일기1",
            date = "2022.02.30",
            deliveryYN ="n",
            commentList = null
        )

    )
    companion object{
        const val TAG: String = "로그"

        fun newInstance() : CalendarWithDiaryFragment {
            return CalendarWithDiaryFragment()
        }
        //7시간
        const val minusCodaTime = 60*60*7*1000

        val ymdFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd")

    }

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var binding: FragmentWritediaryCalendarWithDiaryBinding

    private val myDiaryViewModel: CalendarViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentWritediaryCalendarWithDiaryBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner

        initSwipeRefresh()
        initCalendar()
        initWriteButton()
        observeData()

    }

    private fun observeData(){
        myDiaryViewModel.responseGetMonthDiary.observe(viewLifecycleOwner){
            if (it.isSuccessful) {
                Log.d(TAG, (it.body()?.code ?: 0).toString())
                Log.d(TAG, it.body()?.message ?: "FAIL")
                Log.d(TAG, it.body()?.result?.size.toString())
                Log.d(TAG, it.body()?.result?.get(0)?.content ?: "no")
                Log.d(TAG, (it.body()?.result?.get(0)?.id ?: 0).toString())
                Log.d(TAG, it.body()?.result?.get(0)?.title ?: "no")
                Log.d(TAG, it.body()?.result?.get(0)?.date ?: "no")
                Log.d(TAG, (it.body()?.result?.get(0)?.deliveryYN ?: " "))
                Log.d(TAG, it.body()?.result?.get(0)?.commentList?.isEmpty().toString())
                Toast.makeText(requireContext(), "로그인 성공" + it.body(), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "한 달 일기 불러오기 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initWriteButton() = with(binding){
       writeDiaryLinearLayout.setOnClickListener {
            val intent = Intent(requireContext(), WriteDiaryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initSwipeRefresh() = with(binding){
        //Todo swipe 이벤트 처리
        swipeRefreshLayout.setOnRefreshListener {
            //위의 동작 완료시 리프레쉬 종료
            swipeRefreshLayout.isRefreshing =false
        }
    }

    private fun initCalendar() = with(binding) {
        materialCalendarView.state().edit()
            .setFirstDayOfWeek(Calendar.SUNDAY)
            .setMinimumDate(CalendarDay.from(2021, 0, 1))//캘린더 시작 날짜
            .setMaximumDate(CalendarDay.from(2022, 11, 31))//캘린더 끝 날짜
            .setCalendarDisplayMode(CalendarMode.MONTHS) // 월 달력, 주 달력
            .commit()

        //todo date deprecated 알아보기 , simpledatavforamt도?
        materialCalendarView.selectedDate = CalendarDay.from(Date(Calendar.getInstance().time.time- minusCodaTime))

        val codaTime = Calendar.getInstance()

        codaTime.add(Calendar.HOUR,-7)
        materialCalendarView.selectedDate = CalendarDay.from(codaTime)

        launch(coroutineContext){
            myDiaryViewModel.setResponseGetMonthDiary("2022.02")
        }

        launch(coroutineContext){
            loadData()
        }


        //오늘 날짜 디폴트
        checkSelectedDate(CalendarDay.from(codaTime))

        materialCalendarView.setOnMonthChangedListener { widget, date ->
        }

        materialCalendarView.setOnDateChangedListener { widget, date, selected ->
            checkSelectedDate(date)
        }
    }

    private fun checkSelectedDate(date: CalendarDay) = with(binding){
        //fragment context 사용 ->requireContext()
        val calendar = Calendar.getInstance()
        val codaToday = Calendar.getInstance()
        codaToday.add(Calendar.HOUR, -7)
        calendar.set(date.year, date.month,date.day)

        //미래 날짜면 모두 제거
        if(calendar> codaToday){
            readDiaryConstraintLayout.isVisible = false
            writeDiaryLinearLayout.isVisible = false
            return
        }
        for(diary in diaryList){
//            val ymd = diary.date.split(' ')
            val ymd = diary.date
            val (year,month,day) = ymd.split('.').map{it.toInt()}
            val curDiary = Calendar.getInstance()
            curDiary.set(year,month-1,day)
            curDiary.add(Calendar.HOUR,-7)
            if(ymdFormat.format(curDiary.time)== ymdFormat.format(calendar.time)){
                Toast.makeText(requireContext(), "일기가 있어유!!", Toast.LENGTH_SHORT).show()
                readDiaryConstraintLayout.isVisible = true
                writeDiaryLinearLayout.isVisible = false
                return
            }
        }
        writeDiaryLinearLayout.isVisible = true
        readDiaryConstraintLayout.isVisible = false
    }

    private suspend fun loadData() = withContext(Dispatchers.IO) {

        val aloneDiary = ArrayList<CalendarDay>()
        val commentDiary = ArrayList<CalendarDay>()

        for(selectedDiary in diaryList){
            val ymd = selectedDiary.date
            val calendar = Calendar.getInstance()

            val (year,month,day) = ymd.split('.').map{it.toInt()}
//            val (hour,minute) = hm.split(':').map{it.toInt()}

//            calendar.set(year,month-1,day,hour,minute)
            calendar.set(year,month-1,day)
//            Log.d("aaaaaaa",df.format(calendar.time))

            calendar.add(Calendar.HOUR,-7)
//            Log.d("aaaaaaa",df.format(calendar.time))
            if(selectedDiary.deliveryYN=="y"){
                commentDiary.add(CalendarDay.from(calendar))
            }
            else{
                aloneDiary.add(CalendarDay.from(calendar))
            }
        }

        //그냥 실행하면 background thread로 작업돼서 컴파일 에러
        //livedata의 setValue는 백그라운드 스레드로 작업 불가

        withContext(Dispatchers.Main) {
            myDiaryViewModel.setCommentDiary(commentDiary.toList())
            myDiaryViewModel.setAloneDiary(aloneDiary.toList())
//            Toast.makeText(requireContext(), "${binding.calendarViewModel?.date?.value}", Toast.LENGTH_SHORT).show()
            binding.materialCalendarView.addDecorators(
                AloneDotDecorator(requireContext(), aloneDiary),
                CommentDotDecorator(requireContext(), commentDiary)
            )
        }

    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = FragmentWritediaryCalendarWithDiaryBinding.inflate(layoutInflater)
//    }

//    override fun onAttach(context: Context) {
//        super.onAttach(context)

//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        return binding.root
//    }
}