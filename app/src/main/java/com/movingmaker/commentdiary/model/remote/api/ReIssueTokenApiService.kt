package com.movingmaker.commentdiary.model.remote.api

import com.movingmaker.commentdiary.model.remote.Url
import com.movingmaker.commentdiary.model.remote.request.EmailCodeCheckRequest
import com.movingmaker.commentdiary.model.remote.response.IsSuccessResponse
import com.movingmaker.commentdiary.model.remote.response.LogInResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ReIssueTokenApiService {

    @POST(Url.REISSUE)
    suspend fun reIssueToken(): Response<LogInResponse>

}