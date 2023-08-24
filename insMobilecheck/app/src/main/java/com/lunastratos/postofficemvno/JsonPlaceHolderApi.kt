package com.lunastratos.postofficemvno

import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import java.net.CookieManager


//https://m.epost.go.kr/mobile/mobile.RetrieveMemLoginInfo.comm
interface JsonPlaceHolderApi  {

    @GET("/mobile/login/cafzc008k01.jsp")
    fun loginPage(): Call<ResponseBody>?
}

