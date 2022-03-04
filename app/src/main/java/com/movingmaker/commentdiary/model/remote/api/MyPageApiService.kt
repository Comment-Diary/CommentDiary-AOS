package com.movingmaker.commentdiary.model.remote.api

import com.movingmaker.commentdiary.model.remote.Url
import com.movingmaker.commentdiary.model.remote.request.ChangePasswordRequest
import com.movingmaker.commentdiary.model.remote.response.IsSuccessResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PATCH


interface MyPageApiService {
//    @Headers("Authorization: Bearer ")
    @DELETE(Url.SIGN)
    suspend fun signOut(): Response<IsSuccessResponse>

    @PATCH(Url.SIGN)
    suspend fun changePassword(@Body changePasswordRequest: ChangePasswordRequest): Response<IsSuccessResponse>

    @DELETE(Url.SIGN + Url.LOG_OUT)
    suspend fun logOut(): Response<IsSuccessResponse>
}