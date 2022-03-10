package com.movingmaker.commentdiary.model.remote.api

import com.google.gson.annotations.SerializedName
import com.movingmaker.commentdiary.model.remote.Url
import com.movingmaker.commentdiary.model.remote.request.EditDiaryRequest
import com.movingmaker.commentdiary.model.remote.request.EmailCodeCheckRequest
import com.movingmaker.commentdiary.model.remote.request.SaveDiaryRequest
import com.movingmaker.commentdiary.model.remote.response.DiaryListResponse
import com.movingmaker.commentdiary.model.remote.response.IsSuccessResponse
import com.movingmaker.commentdiary.model.remote.response.SaveDiaryResponse
import retrofit2.Response
import retrofit2.http.*

interface MyDiaryApiService {
    @GET(Url.MONTH_DIARY)
    suspend fun getMonthDiary(@Query("date") date: String): Response<DiaryListResponse>

    @POST(Url.WRITE_DIARY)
    suspend fun saveDiary(@Body saveDiaryRequest: SaveDiaryRequest): Response<SaveDiaryResponse>

    //path variable
    @PATCH("${Url.WRITE_DIARY}/{diaryId}" )
    suspend fun editDiary(@Path("diaryId") diaryId: Long, @Body editDiaryRequest: EditDiaryRequest): Response<IsSuccessResponse>
}