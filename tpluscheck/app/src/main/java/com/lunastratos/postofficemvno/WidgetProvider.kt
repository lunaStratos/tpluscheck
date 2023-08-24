package com.lunastratos.postofficemvno

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import android.widget.TextView
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.net.CookieManager

/**
 * 위젯 데이터 import
 * */
class WidgetProvider : AppWidgetProvider() {

    private val UPDATE = "android.appwidget.action.APPWIDGET_UPDATE"
    private val ENABLED = "android.appwidget.action.APPWIDGET_ENABLED"

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        val action  = intent?.action
        val appWidgetManager = AppWidgetManager.getInstance(context)
        Log.d("위젯터치", "onReceive")
        Log.d("action 이다 : ", action.toString())

        if(action == ENABLED) update(context, appWidgetManager, 0)

    }

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d("위젯터치", "업데이트")
        appWidgetIds!!.forEach {
            update(context, appWidgetManager, it)
        }
        
    }

    fun update(context: Context?, appWidgetManager: AppWidgetManager?, idx: Int) {
        val views = RemoteViews(context!!.packageName, R.layout.info_widget)

        val sharedPreference = context.getSharedPreferences("saveUserInfo", Context.MODE_PRIVATE)
        val getId = sharedPreference.getString("id", "")!!
        val getPw = sharedPreference.getString("pw", "")!!
        val getSelectPhoneNumber = sharedPreference.getString("selectPhoneNumber", "")
        val carrierCompanyPosition = sharedPreference.getInt("carrierCompanyPosition", 0)

        val companyList = arrayOf("선택안함", "LGT", "KT", "SKT")
        val telecomId = companyList[carrierCompanyPosition]

        val manager = AppWidgetManager.getInstance(context)

        val client = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .build()
        val retrofit = Retrofit.Builder().baseUrl("https://www.tplusmobile.com")
            .client(client) //OkHttpClient 연결
            .build()
        val service = retrofit.create(RetrofitInterface::class.java);

        service.setLoginPage(
            getId, getPw, "true"
        ).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if(response.isSuccessful){
                    // 정상적으로 통신이 성공된 경우
                    var result: String? = response.body()!!.string()
                    var code: String? = response.code().toString()
                    val docAfterLogin: Document = Jsoup.parse(result)

                    //로그인 안됨
                    if(docAfterLogin.select("li.u2").select("span.atxt").text().equals("로그인")){
                    }
                    //로그인 됨
                    if(docAfterLogin.select("li.u21").select("span.atxt").text().equals("로그아웃")){

                        Log.d("22", getSelectPhoneNumber.toString())
                        Log.d("22", telecomId.toString())

                        Handler(Looper.getMainLooper()).postDelayed({ }, 20000)

                            //번호선택
                            service.getSelectPhoneNumberPage(getSelectPhoneNumber!! ,telecomId ).enqueue(object : Callback<ResponseBody>{
                                override fun onResponse(
                                    call2: Call<ResponseBody>,
                                    getSelectPhoneNumberResponseBody: Response<ResponseBody>
                                ) {
                                    Log.d("getSelectPhoneNumberResponseBody", getSelectPhoneNumberResponseBody.toString())

                                        service.getDataPage().enqueue(object : Callback<ResponseBody> {
                                            override fun onResponse(
                                                call3: Call<ResponseBody>,
                                                dataPageResponse: Response<ResponseBody>
                                            ) {
                                                Log.d("dataPageResponse", dataPageResponse.toString())
                                                Log.d("dataPageResponse", dataPageResponse.code().toString())
                                                var code: String? = dataPageResponse.code().toString()
                                                var html: String? = dataPageResponse.body()!!.string()
                                                val doc: Document = Jsoup.parse(html)

                                                // 타이틀
                                                val phoneVoiceTitle = doc.select("div.voice").select("span.tit").text()
                                                val phoneDataTitle =  doc.select("div.data").select("span.tit").text()
                                                val phoneMmsTitle = doc.select("div.mms").select("span.tit").text()

                                                // 비율
                                                val phoneVoiceLimit = doc.select("div.voice").select("span.rate").text().split("/")[1]
                                                val phoneDataLimit =  doc.select("div.data").select("span.rate").text().split("/")[1]
                                                val phoneMmsLimit = doc.select("div.mms").select("span.rate").text().split("/")[1]

                                                //비율2
                                                val phoneVoice = doc.select("div.voice").select("span.rate").text().split("/")[0]
                                                val phoneData =  doc.select("div.data").select("span.rate").text().split("/")[0]
                                                val phoneMms = doc.select("div.mms").select("span.rate").text().split("/")[0]

                                                val textArray = arrayOf("데이터", "음성", "문자")
                                                val textTitleArray = arrayOf(
                                                    "${phoneDataTitle},${phoneDataLimit},${phoneData}",
                                                    "${phoneVoiceTitle},${phoneVoiceLimit},${phoneVoice}",
                                                    "${phoneMmsTitle},${phoneMmsLimit},${phoneMms}"
                                                )

                                                // 조합
                                                for ((index, item )in textArray.withIndex()) {
                                                    for ((titleIndex, titleItem) in textTitleArray.withIndex()) {

                                                        if (titleItem.indexOf(item) != -1) {//일치하는게 있다면
                                                            Log.d(
                                                                "phoneMmsTitle",
                                                                "${index} / ${item} , ${titleItem}"
                                                            )
                                                            val titleItemArray =
                                                                titleItem.split(",")
                                                            when (index) {
                                                                0 -> {
                                                                    // 타이틀텍스트에 [데이터]가 들어간 경우
                                                                    if (titleItem.indexOf("GB") != -1) {
                                                                        val dataLimit = (titleItemArray[1].toDouble()*1024).toInt()
                                                                        val dataUse = (titleItemArray[2].toDouble()*1024).toInt()
                                                                        views.setTextViewText(R.id.dataWidgetText, dataUse.toString() ) // 데이터
                                                                    }
                                                                    //그냥 MB인 경우
                                                                    if (titleItem.indexOf("MB") != -1) views.setTextViewText(R.id.dataWidgetText, titleItemArray[2]) // 데이터

                                                                }
                                                                1 -> { // 타이틀텍스트에 [음성]이 들어간 경우
                                                                    views.setTextViewText(R.id.voiceWidgetText, titleItemArray[2]) // 음성
                                                                }
                                                                2 -> { // 타이틀텍스트에 [문자]가 들어간 경우
                                                                    views.setTextViewText(R.id.mmsWidgetText, titleItemArray[2])   // 문자
                                                                }
                                                            }
                                                        }
                                                    }
                                                }


                                                appWidgetManager!!.updateAppWidget(idx, views)

                                            }

                                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                                TODO("Not yet implemented")
                                            }

                                        })


                                }

                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    TODO("Not yet implemented")
                                }
                            }) // getSelectPhoneNumberPage

                    }

                }else{
                    // 통신이 실패한 경우(응답코드 3xx, 4xx 등)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 통신 실패 (인터넷 끊킴, 예외 발생 등 시스템적인 이유)
            }
        })

    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.d("위젯터치", "onAppWidgetOptionsChanged")
        update(context, appWidgetManager, appWidgetId)

    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)

    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
    }

    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        Log.d("위젯터치", "onRestored")

    }


}