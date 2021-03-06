package com.movingmaker.commentdiary.view.main.mydiary

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.movingmaker.commentdiary.CodaApplication
import com.movingmaker.commentdiary.R
import com.movingmaker.commentdiary.base.BaseFragment
import com.movingmaker.commentdiary.databinding.FragmentMydiaryWritediaryBinding
import com.movingmaker.commentdiary.global.CodaSnackBar
import com.movingmaker.commentdiary.model.entity.Diary
import com.movingmaker.commentdiary.model.remote.RetrofitClient
import com.movingmaker.commentdiary.model.remote.request.EditDiaryRequest
import com.movingmaker.commentdiary.model.remote.request.SaveDiaryRequest
import com.movingmaker.commentdiary.util.DateConverter
import com.movingmaker.commentdiary.view.main.MainActivity
import com.movingmaker.commentdiary.viewmodel.FragmentViewModel
import com.movingmaker.commentdiary.viewmodel.mydiary.MyDiaryViewModel
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class WriteDiaryFragment : BaseFragment(), CoroutineScope, SelectDiaryTypeListener {

    override val TAG: String = WriteDiaryFragment::class.java.simpleName

    private lateinit var binding: FragmentMydiaryWritediaryBinding
    private lateinit var diaryTypeBottomSheet: SelectDiaryTypeBottomSheet
    private val myDiaryViewModel: MyDiaryViewModel by activityViewModels()
    private val fragmentViewModel: FragmentViewModel by activityViewModels()

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    companion object{
        fun newInstance() : WriteDiaryFragment {
            return WriteDiaryFragment()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentMydiaryWritediaryBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.myDiaryviewModel = myDiaryViewModel
        //??? ??? ??? ?????? ????????? ???????????? ?????? ????????? ????????? ????????????????????? ????????????
//        binding.lifecycleOwner = this
        binding.lifecycleOwner = viewLifecycleOwner
        fragmentViewModel.setHasBottomNavi(false)

        //initview??? ???????????? ?????? ??????
        //changeview??? ????????? ?????? ??????
//        initViews()
        observeDatas()
        return binding.root
    }

    private fun observeDatas() = with(binding) {

        fragmentViewModel.fragmentState.observe(viewLifecycleOwner){ curFragment->
            if(curFragment=="writeDiary"){
                initViews()
                initToolbar()
            }
        }

        //????????? ??????
        myDiaryViewModel.responseDeleteDiary.observe(viewLifecycleOwner){ response ->
            binding.loadingBar.isVisible = false
            if(response.isSuccessful){
                if(fragmentViewModel.beforeFragment.value=="gatherDiary"){
                    fragmentViewModel.setFragmentState("gatherDiary")
                }
                else{
                    fragmentViewModel.setFragmentState("myDiary")
                }
                CodaSnackBar.make(binding.root, "????????? ?????????????????????.").show()
            }
            else{
                response.errorBody()?.let{ errorBody->
                    RetrofitClient.getErrorResponse(errorBody)?.let {
                        if (it.status == 401) {
                            Toast.makeText(requireContext(), "?????? ???????????? ?????????.", Toast.LENGTH_SHORT)
                                .show()
                            CodaApplication.getInstance().logOut()
                        } else {
                            CodaSnackBar.make(binding.root, "?????? ????????? ?????????????????????.").show()
                        }
                    }
                }
            }

        }

        //????????? ?????? ??? ??????, ????????? ?????? ??? ??? ??????
        myDiaryViewModel.responseSaveDiary.observe(viewLifecycleOwner){
            binding.loadingBar.isVisible = false
            if(it.isSuccessful){
                //?????? ??? ?????? ????????? ?????? ?????????
                if(myDiaryViewModel.selectedDiary.value!!.deliveryYN=='N')
                    CodaSnackBar.make(binding.root, "????????? ?????????????????????.").show()
                //???????????? ??????
                myDiaryViewModel.setSelectedDiary(
                    Diary(
                        id = it.body()!!.result.id,
                        title = diaryHeadEditText.text.toString(),
                        content = diaryContentEditText.text.toString(),
                        date = myDiaryViewModel.selectedDiary.value!!.date,
                        deliveryYN = myDiaryViewModel.selectedDiary.value!!.deliveryYN,
                        tempYN = myDiaryViewModel.selectedDiary.value!!.tempYN,
                        commentList = null
                    )
                )

                //?????? ??? ????????? ??????
                if(myDiaryViewModel.selectedDiary.value!!.deliveryYN=='N'){
                    //??????,?????? ?????????, ?????? ?????????, ????????? ?????? ??????
                    deleteButton.isVisible = true
                    editButton.isVisible = true
                    saveButton.isVisible = false
                    diaryHeadEditText.isEnabled = false
                    diaryContentEditText.isEnabled = false
                }
                else if(myDiaryViewModel.selectedDiary.value!!.tempYN=='Y'){
                    deleteButton.isVisible = true
                    editButton.isVisible = true
                    saveButton.isVisible = true
                    diaryHeadEditText.isEnabled = false
                    diaryContentEditText.isEnabled = false
                }
                //????????? ????????? ?????? ??????
                else{
//                    parentFragmentManager.popBackStack()
                    fragmentViewModel.setFragmentState("commentDiaryDetail")
                }
            }
            else{
                it.errorBody()?.let{ errorBody->
                    RetrofitClient.getErrorResponse(errorBody)?.let {
                        if (it.status == 401) {
                            Toast.makeText(requireContext(), "?????? ???????????? ?????????.", Toast.LENGTH_SHORT)
                                .show()
                            CodaApplication.getInstance().logOut()
                        } else {
                            CodaSnackBar.make(binding.root, "?????? ????????? ?????????????????????.").show()
                        }
                    }
                }
            }
        }

        //????????? ??????, ?????? ?????? ?????? ??????
        myDiaryViewModel.responseEditDiary.observe(viewLifecycleOwner){
            binding.loadingBar.isVisible = false
            if(it.isSuccessful){
                CodaSnackBar.make(binding.root, "????????? ?????????????????????.").show()
                //???????????? ??????
                myDiaryViewModel.setSelectedDiary(
                    Diary(
                        id = myDiaryViewModel.selectedDiary.value!!.id,
                        title = diaryHeadEditText.text.toString(),
                        content = diaryContentEditText.text.toString(),
                        date = myDiaryViewModel.selectedDiary.value!!.date,
                        deliveryYN = myDiaryViewModel.selectedDiary.value!!.deliveryYN,
                        tempYN = myDiaryViewModel.selectedDiary.value!!.tempYN,
                        commentList = myDiaryViewModel.selectedDiary.value!!.commentList
                    )
                )

                //?????? ?????? ????????????
                if(myDiaryViewModel.selectedDiary.value!!.tempYN=='Y'){
                    saveButton.isVisible = true
                    saveButton.text = getString(R.string.send_text)
                    deleteButton.isVisible = true
                    editButton.isVisible = true
                    diaryHeadEditText.isEnabled = false
                    diaryContentEditText.isEnabled = false
                    diaryContentEditText.hint = getString(R.string.write_diary_content_100_hint)
                }
                else{
                    //?????? ?????? ????????????
                    if(myDiaryViewModel.selectedDiary.value!!.deliveryYN=='N'){
                        saveButton.isVisible = false
                        saveButton.text = getString(R.string.store_text)
                        deleteButton.isVisible = true
                        editButton.isVisible = true
                        diaryHeadEditText.isEnabled = false
                        diaryContentEditText.isEnabled = false
                        diaryContentEditText.hint = getString(R.string.write_diary_content_hint)
                    }
                    //?????? ?????? ??????(edit api)??????
                    else{
                        fragmentViewModel.setBeforeFragment("writeDiary")
                        fragmentViewModel.setFragmentState("commentDiaryDetail")
                    }
                }
            }
            else{
                it.errorBody()?.let{ errorBody->
                    RetrofitClient.getErrorResponse(errorBody)?.let {
                        if (it.status == 401) {
                            Toast.makeText(requireContext(), "?????? ???????????? ?????????.", Toast.LENGTH_SHORT)
                                .show()
                            CodaApplication.getInstance().logOut()
                        } else {
                            CodaSnackBar.make(binding.root, "?????? ????????? ?????????????????????.").show()
                        }
                    }
                }
            }
        }

    }


    @SuppressLint("ResourceAsColor")
    private fun changeViews() = with(binding){
        Log.d(TAG, "changeViews: ${myDiaryViewModel.selectedDiary.value!!}")
        Log.d(TAG, "changeViews: ${myDiaryViewModel.selectedDiary.value!!.tempYN}")
        Log.d(TAG, "changeViews: ${myDiaryViewModel.saveOrEdit.value}")
        saveButton.setTextColor(ContextCompat.getColor(requireContext(),R.color.background_ivory))
        //????????? ?????? ??????
        if(myDiaryViewModel.selectedDiary.value!!.id!=null) {
            //????????? ????????? ?????? ??????
            if (myDiaryViewModel.selectedDiary.value!!.deliveryYN == 'N') {
                //???????????? ???????????? ?????? ?????????
                if (myDiaryViewModel.saveOrEdit.value == "edit") {
                    saveButton.setBackgroundResource(R.drawable.background_pure_green_radius_30)
                    saveButton.isVisible = true
                    editButton.isVisible = false
                    deleteButton.isVisible = false
                    diaryContentEditText.isEnabled = true
                    diaryHeadEditText.isEnabled = true
                }
                //?????? ?????? ??????
                else {
                    saveButton.isVisible = false
                    editButton.isVisible = true
                    deleteButton.isVisible = true
                    diaryContentEditText.isEnabled = false
                    diaryHeadEditText.isEnabled = false
                }
                saveLocalButton.isVisible = false
                diaryUploadServerYetTextView.isVisible = false
                writeCommentDiaryTextLimitTextView.isVisible = false

            }
            //????????????????????? ??????
            else {
                myDiaryViewModel.setSaveOrEdit("save")
                writeCommentDiaryTextLimitTextView.isVisible = true
                if (myDiaryViewModel.selectedDiary.value!!.content != "") {
                    saveButton.text = getString(R.string.send_text)
                    saveButton.isVisible = true
                    saveButton.setBackgroundResource(R.drawable.background_pure_green_radius_30)
                    editButton.isVisible = true
                    deleteButton.isVisible = true
                    saveLocalButton.isVisible = false
                    diaryUploadServerYetTextView.isVisible = true
                    diaryContentEditText.isEnabled = false
                    diaryHeadEditText.isEnabled = false
                    //?????? ?????? ??????
                    if (DateConverter.ymdToDate(myDiaryViewModel.selectedDiary.value!!.date) <=
                        DateConverter.getCodaToday().minusDays(2)
                    ) {
                        diaryUploadServerYetTextView.text =
                            getString(R.string.already_pass_diary_send_time)
                        editButton.isVisible = false
                        saveButton.setBackgroundResource(R.drawable.background_light_brown_radius_30)
                        saveButton.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.text_brown
                            )
                        )
                        saveButton.isEnabled = false
                    } else {
                        diaryUploadServerYetTextView.text =
                            getString(R.string.upload_temp_time_comment_diary)
                        saveButton.isEnabled = true
                    }
                }
            }
        }
        //????????? ?????? ??????
        else{
            diaryContentEditText.isEnabled = true
            diaryHeadEditText.isEnabled = true
            saveButton.isVisible = true
            saveButton.setBackgroundResource(R.drawable.background_pure_green_radius_30)
            saveButton.isEnabled = true
            editButton.isVisible = false
            deleteButton.isVisible = false
            //?????? ?????? ?????? ??????
            if(myDiaryViewModel.selectedDiary.value!!.deliveryYN=='N'){
                saveLocalButton.isVisible = false
                saveButton.text = getString(R.string.store_text)
                writeCommentDiaryTextLimitTextView.isVisible = false
                diaryUploadServerYetTextView.isVisible = false
                diaryContentEditText.hint = getString(R.string.write_diary_content_hint)
            }
            //????????? ?????? ?????? ??????
            else{
                saveLocalButton.isVisible = true
                saveButton.text = getString(R.string.send_text)
                writeCommentDiaryTextLimitTextView.isVisible = true
                diaryUploadServerYetTextView.isVisible = true
                diaryContentEditText.hint = getString(R.string.write_diary_content_100_hint)
            }
        }
        saveButton.setOnClickListener {
            Log.d(TAG, "changeViews: savebuttonclick ${myDiaryViewModel.saveOrEdit.value}")
            //??????,?????? ??????????????? ??????
            if(diaryHeadEditText.text.isEmpty()){
                CodaSnackBar.make(binding.root, "????????? ????????? ?????????.").show()
                return@setOnClickListener
            }
            else if(diaryContentEditText.text.isEmpty()){
                CodaSnackBar.make(binding.root, "????????? ????????? ?????????.").show()
                return@setOnClickListener
            }
            when(myDiaryViewModel.saveOrEdit.value){
                "save"->{
                    when(myDiaryViewModel.selectedDiary.value!!.deliveryYN){
                        //????????? ?????? ?????? ?????? ?????? ??????
                        'Y'->{
                            //??????????????? ????????? ?????? ????????? ????????? ?????? ????????? ???????????? ?????????
                            showDialog("save")
                        }
                        //?????? ????????? ?????? ??????
                        'N'-> {
                            //?????? ??????(api ??????)
                            binding.loadingBar.isVisible = true
                            launch(coroutineContext) {
                                withContext(Dispatchers.IO) {
                                    myDiaryViewModel.setResponseSaveDiary(
                                        SaveDiaryRequest(
                                            title = diaryHeadEditText.text.toString(),
                                            content = diaryContentEditText.text.toString(),
                                            date = myDiaryViewModel.selectedDiary.value!!.date,
                                            deliveryYN = myDiaryViewModel.selectedDiary.value!!.deliveryYN,
                                            tempYN = 'N'
                                        )
                                    )
                                }
                                saveButton.isVisible = false
                                editButton.isVisible = true
                                deleteButton.isVisible = true
                            }
                        }
                    }
                }
                //?????? ??????
                "edit"-> {
                    when(myDiaryViewModel.selectedDiary.value!!.deliveryYN){
                        //???????????? ?????? ?????? ?????? ??????
                        'Y'->{
                            showDialog("save")
                        }
                        //?????? ?????? ??????
                        'N'->{
                            //edit api??????
                            //????????? ?????? observer?????? view, myviewmodel.selecteddiary ??????
                            launch(coroutineContext) {
                                binding.loadingBar.isVisible = true
                                withContext(Dispatchers.IO) {
                                    myDiaryViewModel.selectedDiary.value!!.id?.let { id ->
                                        myDiaryViewModel.setResponseEditDiary(
                                            diaryId = id,
                                            editDiaryRequest = EditDiaryRequest(
                                                diaryHeadEditText.text.toString(),
                                                diaryContentEditText.text.toString(),
                                                'N'
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initViews() = with(binding) {

        //?????? ????????? ?????? ????????????
        myDiaryViewModel.setCommentDiaryTextCount(diaryContentEditText.text.length)

        //??????????????? ????????? ?????? ?????? ?????? ?????????
        if (myDiaryViewModel.selectedDiary.value!!.id ==null && myDiaryViewModel.selectedDiary.value!!.content=="") {
            //?????? ????????? ??????????????? ??????
            if(DateConverter.ymdToDate(myDiaryViewModel.selectedDiary.value!!.date) == DateConverter.getCodaToday()){
                //????????????
                diaryTypeBottomSheet = SelectDiaryTypeBottomSheet(this@WriteDiaryFragment)
                diaryTypeBottomSheet.show(parentFragmentManager, "selectDiaryBottomSheet")
                diaryTypeBottomSheet.isCancelable = false
            }
            //?????? ????????? ?????? ?????? ????????? ??????
            else{
                myDiaryViewModel.setDeliveryYN('N')
            }

        }

        //????????? ?????????
        diaryContentEditText.addTextChangedListener {
            myDiaryViewModel.setCommentDiaryTextCount(diaryContentEditText.text.length)
        }

        changeViews()
    }

    private fun initToolbar() = with(binding){
        //????????????
        backButton.setOnClickListener {
            myDiaryViewModel.setSaveOrEdit("save")
            //?????? ????????? ??????, ?????? ???????????? ????????? ??? ??????
            if(diaryContentEditText.isEnabled &&
                (diaryContentEditText.text.toString() !=myDiaryViewModel.selectedDiary.value!!.content||
                        diaryHeadEditText.text.toString() !=myDiaryViewModel.selectedDiary.value!!.title)
            ){
                showBackDialog()
            }
            else{
                if(fragmentViewModel.beforeFragment.value=="gatherDiary"){
                    fragmentViewModel.setFragmentState("gatherDiary")
                }
                else {
                    fragmentViewModel.setFragmentState("myDiary")
                }
            }

        }

        //?????? ??????
        saveLocalButton.setOnClickListener {
            //??????,?????? ??????????????? ??????
            if(diaryHeadEditText.text.isEmpty()){
                CodaSnackBar.make(binding.root, "????????? ????????? ?????????.").show()
                return@setOnClickListener
            }
            else if(diaryContentEditText.text.isEmpty()){
                CodaSnackBar.make(binding.root, "????????? ????????? ?????????.").show()
                return@setOnClickListener
            }
            binding.loadingBar.isVisible = true
            //insert ?????? ????????? ????????? ????????? ????????? ??? ??? ??????????
            if(myDiaryViewModel.selectedDiary.value!!.title==""){
                //??????
                myDiaryViewModel.selectedDiary.value!!.tempYN = 'Y'
                launch(coroutineContext) {
                    withContext(Dispatchers.IO) {
                        myDiaryViewModel.setResponseSaveDiary(
                            SaveDiaryRequest(
                                title = diaryHeadEditText.text.toString(),
                                content = diaryContentEditText.text.toString(),
                                date = myDiaryViewModel.selectedDiary.value!!.date,
                                deliveryYN = myDiaryViewModel.selectedDiary.value!!.deliveryYN,
                                tempYN = 'Y'
                            )
                        )
                    }
                }
            }
            else{
                //??????
                launch(coroutineContext) {
                    withContext(Dispatchers.IO) {
                        myDiaryViewModel.selectedDiary.value!!.id?.let { id ->
                            myDiaryViewModel.setResponseEditDiary(
                                diaryId = id,
                                editDiaryRequest = EditDiaryRequest(
                                    diaryHeadEditText.text.toString(),
                                    diaryContentEditText.text.toString(),
                                    myDiaryViewModel.selectedDiary.value!!.tempYN
                                )
                            )
                        }
                    }
                }
            }
            deleteButton.isVisible = true
            editButton.isVisible = true
            saveLocalButton.isVisible = false
            diaryContentEditText.isEnabled = false
            diaryHeadEditText.isEnabled = false
        }

        deleteButton.setOnClickListener {
            //???????????? ?????? or ??????api
            showDialog("delete")
        }

        editButton.setOnClickListener {
            //??????????????? ?????? api???
            myDiaryViewModel.setSaveOrEdit("edit")

            binding.deleteButton.isVisible = false
            binding.editButton.isVisible = false
            binding.saveButton.isVisible = true
            binding.diaryContentEditText.isEnabled = true
            binding.diaryHeadEditText.isEnabled = true
            if(myDiaryViewModel.selectedDiary.value!!.deliveryYN =='Y'){
                binding.saveButton.text = getString(R.string.send_text)
                binding.saveLocalButton.isVisible = true
            }
            else{
                diaryUploadServerYetTextView.isVisible = false
                binding.saveButton.text = getString(R.string.store_text)
            }
        }
    }

    private fun showBackDialog(){
        val dialogView = Dialog(requireContext())
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.setContentView(R.layout.dialog_mydiary_writediary)
        dialogView.setCancelable(false)

        dialogView.show()


        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val noticeTextView = dialogView.findViewById<TextView>(R.id.noticeTextView)
        noticeTextView.text = getString(R.string.write_diary_back)

        submitButton.setOnClickListener {
            fragmentViewModel.setFragmentState("myDiary")
            dialogView.dismiss()
        }

        cancelButton.setOnClickListener {
            dialogView.dismiss()
        }
    }

    private fun showDialog(deleteOrSave: String) {
        val dialogView = Dialog(requireContext())
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.setContentView(R.layout.dialog_mydiary_writediary)
        dialogView.setCancelable(false)

        dialogView.show()


        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val noticeTextView = dialogView.findViewById<TextView>(R.id.noticeTextView)
        when(deleteOrSave){
            "save"->{
                noticeTextView.text = getString(R.string.diary_send_warning)
            }
            "delete"->{
                noticeTextView.text = getString(R.string.diary_delete_warning)
            }
        }
        submitButton.setOnClickListener {
            when(deleteOrSave){
                "save"->{
                    if(binding.diaryContentEditText.text.toString().length<100){
                        CodaSnackBar.make(binding.root, "????????? 100??? ?????? ????????? ?????????.").show()
                        dialogView.dismiss()
                        return@setOnClickListener
                    }

                    // ????????? ????????? ?????????, ???????????? ??????????????? ?????? ??? ????????? ?????? api?????? ??? ?????????2??? ??? ???????????? ??????????????? ??????

                    launch(coroutineContext) {
                        withContext(Dispatchers.IO) {
                            //??????????????? ?????? ????????? create
                            if (myDiaryViewModel.selectedDiary.value!!.tempYN == 'N' || myDiaryViewModel.selectedDiary.value!!.tempYN == ' ') {
                                myDiaryViewModel.setResponseSaveDiary(
                                    SaveDiaryRequest(
                                        title = binding.diaryHeadEditText.text.toString(),
                                        content = binding.diaryContentEditText.text.toString(),
                                        date = myDiaryViewModel.selectedDiary.value!!.date,
                                        deliveryYN = myDiaryViewModel.selectedDiary.value!!.deliveryYN,
                                        tempYN = 'N'
                                    )
                                )
                            } else {
                                myDiaryViewModel.selectedDiary.value!!.tempYN = 'N'
                                myDiaryViewModel.selectedDiary.value!!.id?.let { id ->
                                    myDiaryViewModel.setResponseEditDiary(
                                        diaryId = id,
                                        editDiaryRequest = EditDiaryRequest(
                                            binding.diaryHeadEditText.text.toString(),
                                            binding.diaryContentEditText.text.toString(),
                                            //????????? N?????? ???
                                            'N'
                                        )
                                    )
                                }
                            }
                        }
                        dialogView.dismiss()
                        showCircleDialog()
                    }
                }
                "delete"->{
                    //?????? ?????? ?????? ??????
                    launch(coroutineContext) {
                        binding.loadingBar.isVisible = true
                        withContext(Dispatchers.IO) {
                            myDiaryViewModel.selectedDiary.value!!.id?.let {
                                myDiaryViewModel.setResponseDeleteDiary(it)
                            }
                        }
                        dialogView.dismiss()
                    }
                }
            }
        }

        cancelButton.setOnClickListener {
            dialogView.dismiss()
        }
    }
    private suspend fun showCircleDialog() {

        val dialogView = Dialog(requireContext())
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.setContentView(R.layout.dialog_mydiary_success_notice)
        dialogView.setCancelable(false)
        dialogView.show()
        delay(2000L)
        dialogView.dismiss()
    }

    override fun onSelectDiaryTypeListener(deliveryYN: Char) {
        changeViews()
    }
}