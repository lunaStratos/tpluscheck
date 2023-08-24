package com.lunastratos.postofficemvno

import android.util.JsonReader
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface RetrofitInterface {

    // 최초 로그인 
    @FormUrlEncoded
    @POST("adm/member/login_check.php")
    fun setLoginPage(
        @Field("id") mberId: String,
        @Field("passwd") password: String,
        @Field("selidx") selidx : String,
        @Field("prev") prev : String,
    ): Call<ResponseBody>

    //데이터 있는 부분


    @GET("member/used_info.php")
    fun getDataPage(): Call<ResponseBody>

    @FormUrlEncoded
    @POST("adm/api/telecentro.php")
    fun postDataPage(@Field("mode") useNum: String): Call<ResponseBody>

    //데이터 있는 부분
    @FormUrlEncoded
    @POST("member/my_item_info.php")
    fun getSelectPhoneNumberPage(
        @Field("useNum") useNum: String,
        @Field("telecomId") telecomId: String,
    ): Call<ResponseBody>
}