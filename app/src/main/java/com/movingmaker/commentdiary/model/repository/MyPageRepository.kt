package com.movingmaker.commentdiary.model.repository

import android.util.Log
import com.movingmaker.commentdiary.model.remote.RetrofitClient
import com.movingmaker.commentdiary.model.remote.response.IsSuccessResponse
import retrofit2.Response

class MyPageRepository {

    companion object{
        val INSTANCE = MyPageRepository()
        val TAG = "레트로핏 로그"
    }

    suspend fun signOut(): Response<IsSuccessResponse> {
        Log.d(TAG, "signOut: 불린겨 뭐여?????????????")
        return RetrofitClient.myPageApiService.signOut()
    }

}