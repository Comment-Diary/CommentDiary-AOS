package com.movingmaker.commentdiary.model.remote

import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.movingmaker.commentdiary.BuildConfig
import com.movingmaker.commentdiary.CodaApplication
import com.movingmaker.commentdiary.model.remote.api.*
import com.movingmaker.commentdiary.model.remote.response.ErrorResponse
import com.movingmaker.commentdiary.model.repository.ReIssueTokenRepository
import com.movingmaker.commentdiary.util.RetrofitHeaderCondition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val TAG: String? = "Interceptor"
    val onboardingApiService: OnboardingApiService by lazy {
        getRetrofit(RetrofitHeaderCondition.NO_HEADER).create(OnboardingApiService::class.java)
    }

    val myPageApiService: MyPageApiService by lazy {
        getRetrofit(RetrofitHeaderCondition.BEARER).create(MyPageApiService::class.java)
    }

    val myDiaryApiService: MyDiaryApiService by lazy{
        getRetrofit(RetrofitHeaderCondition.BEARER).create(MyDiaryApiService::class.java)
    }

    val reIssueTokenApiService: ReIssueTokenApiService by lazy{
        getRetrofit(RetrofitHeaderCondition.TWO_HEADER).create(ReIssueTokenApiService::class.java)
    }

    val logOutApiService: LogOutApiService by lazy{
        getRetrofit(RetrofitHeaderCondition.ONE_HEADER).create(LogOutApiService::class.java)
    }

    val gatherDiaryApiService: GatherDiaryApiService by lazy{
        getRetrofit(RetrofitHeaderCondition.BEARER).create(GatherDiaryApiService::class.java)
    }

    val receivedDiaryApiService: ReceivedDiaryApiService by lazy{
        getRetrofit(RetrofitHeaderCondition.BEARER).create(ReceivedDiaryApiService::class.java)
    }


//    private fun getSimpleRetrofit(): Retrofit {
//        return Retrofit.Builder()
//            .baseUrl(Url.CODA_BASE_URL)
//            .addConverterFactory(
//                GsonConverterFactory.create(
//                    GsonBuilder()
//                        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
//                        .create()
//                )
//            )
//            .client(buildOkHttpClient())
//            .build()
//    }

     private fun getRetrofit(headerCondition: String): Retrofit {
//         var gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl(Url.BASE_URL)
            .client(buildHeaderOkHttpClient(headerCondition))
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                        .setLenient()
                        .create()
                )
            )
            .build()
    }
    //?????? + ??????
    private fun buildHeaderOkHttpClient(headerCondition: String): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        if (BuildConfig.DEBUG) {
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            loggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
        }
        //todo Authenticator??? ?????????
        when(headerCondition) {
            RetrofitHeaderCondition.NO_HEADER->{
                return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
            }
            RetrofitHeaderCondition.BEARER->{
                return OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor)
//            .authenticator(TokenAuthenticator())
                    .addInterceptor(BearerInterceptor())
                    .build()
            }
            RetrofitHeaderCondition.ONE_HEADER->{
                return OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(OneHeaderInterceptor())
                    .build()
            }
            RetrofitHeaderCondition.TWO_HEADER->{
                return OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(TwoHeaderInterceptor())
                    .build()
            }
        }
        return OkHttpClient.Builder().build()
    }


//    private fun buildOkHttpClient(): OkHttpClient {
//        val interceptor = HttpLoggingInterceptor()
//        if (BuildConfig.DEBUG) {
//            interceptor.level = HttpLoggingInterceptor.Level.BODY
//        } else {
//            interceptor.level = HttpLoggingInterceptor.Level.NONE
//        }
//
//        return OkHttpClient.Builder()
//            .connectTimeout(5, TimeUnit.SECONDS)
//            .addInterceptor(interceptor)
//            .build()
//    }

    //?????? ???????????? api??? ??????????????? ????????? ?????? ????????? ??????x
    class OneHeaderInterceptor: Interceptor{
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {

            val accessToken = runBlocking {
                CodaApplication.getInstance().getDataStore().accessToken.first()
            }
            val newRequest = chain.request().newBuilder().addHeader("X-AUTH-TOKEN", accessToken).build()
            Log.d(TAG, "intercept: $newRequest")
            return chain.proceed(newRequest)
        }
    }

    /*
    * bearer ?????? ????????? api ????????? accessToken???????????? ??????
    * ???????????? ????????? ????????? api ??????
    * ????????? api?????? ??? refreshToken??? ???????????? ????????? error 401~404
    * ??? ?????? ????????????
    * refreshToken??? ??????????????? ??????????????? accessToken????????? ??? ?????? api ?????? ??????
    * */
    class BearerInterceptor: Interceptor {
        //todo ?????? ????????? ???????????? ?????? ??????
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {

            val accessTokenExpiresIn  = runBlocking {
                CodaApplication.getInstance().getDataStore().accessTokenExpiresIn.first()
            }
            var accessToken = ""

            //todo ????????????????????? ?????? ???????????? ?????? ??? ???????????? ?????????.. 401?????? 402
            if(accessTokenExpiresIn <= System.currentTimeMillis()){
                accessToken = runBlocking {
                    //?????? ?????? api ??????
                    val response = ReIssueTokenRepository.INSTANCE.reIssueToken()
                    val errorResponse = response.errorBody()?.let { getErrorResponse(it) }
                    //refreshToken  ????????? ??????
                    if(errorResponse?.status in 401 .. 404){
                        CodaApplication.getInstance().logOut()
                    }else {
                        try {
                            CodaApplication.getInstance().getDataStore().insertAuth(
                                response.body()!!.result.accessToken,
                                response.body()!!.result.refreshToken,
                                CodaApplication.customExpire
//                                response.body()!!.result.accessTokenExpiresIn
                            )
                        } catch (e: Exception) {
                        }
                    }
                    response.body()?.result?.accessToken?: "Empty Token"
                }
            }
            else{
                accessToken = runBlocking {
                    CodaApplication.getInstance().getDataStore().accessToken.first()
                }
            }
            val newRequest = chain.request().newBuilder().addHeader("Authorization", "Bearer ${accessToken}")
                .build()
            Log.d(TAG, "intercept: $newRequest")
            return chain.proceed(newRequest)
        }
    }


    class TwoHeaderInterceptor: Interceptor {
        //refreshtoken??? ????????????????? ????????????

        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            //?????? ??? ?????????
            val refreshToken  = runBlocking {
                CodaApplication.getInstance().getDataStore().refreshToken.first()
            }
            val accessToken = runBlocking {
                CodaApplication.getInstance().getDataStore().accessToken.first()
            }
            //??????????????? ????????? ?????? ???????????? ???

            val newRequest = chain.request().newBuilder().addHeader("X-AUTH-TOKEN", accessToken).addHeader("REFRESH-TOKEN", refreshToken)
                .build()
            Log.d(TAG, "intercept: $newRequest")
            return chain.proceed(newRequest)
        }
    }

    fun getErrorResponse(errorBody: ResponseBody): ErrorResponse? {
//      errorBody????????? ?????? ??? ?????? errorBody?????? ???????????? null??? ?????????.. ??????????  Log.d("errorbody??????????????????", errorBody.string())
        return getRetrofit(RetrofitHeaderCondition.TWO_HEADER).responseBodyConverter<ErrorResponse>(
            ErrorResponse::class.java,
            ErrorResponse::class.java.annotations
        ).convert(errorBody)
    }

    //    class TokenAuthenticator : Authenticator {
//        override fun authenticate(route: Route?, response: Response): Request? {
////            Log.d("111111111111", "Token " + "token")
//            if (response.request.header("Authorization") != null) {
////                Log.d("2222222222222", "Token " + "token")
//                return null
//            }
//
//            return response.request.newBuilder().header("Authorization", "Bearer ").build()
//        }
//    }
////    OkHttpClient.Builder().authenticator(TokenAuthenticator()).build()
}