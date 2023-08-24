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
import org.json.JSONObject
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
        val retrofit = Retrofit.Builder().baseUrl("https://insmobile.co.kr/")
            .client(client) //OkHttpClient 연결
            .build()
        val service = retrofit.create(RetrofitInterface::class.java);

        service.setLoginPage(
            getId, getPw, "", ""
        ).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if(response.isSuccessful){

                    service.getDataPage().enqueue( object :Callback<ResponseBody>{
                        override fun onResponse(
                            call2: Call<ResponseBody>,
                            dataPageResponse: Response<ResponseBody>
                        ) {
                            var html: String = dataPageResponse.body()!!.string()
                            val doc: Document = Jsoup.parse(html)

                            Log.d("getDataPage", " getDataPage=>  " + doc.select("div.my_item_section2_box.clearfix"))


                            service.postDataPage("check_used").enqueue( object :Callback<ResponseBody>{
                                override fun onResponse(
                                    call3: Call<ResponseBody>,
                                    dataPageResponse: Response<ResponseBody>
                                ) {
                                    var html3: String = dataPageResponse.body()!!.string()
                                    val doc: Document = Jsoup.parse(html3)

                                    Log.d("html3", "html3 : " + html3)
                                    val jsonObject = JSONObject(html3)

                                    if(jsonObject.getString("RET_MSG").equals("SUCCESS")){
                                        // 남음
                                        val phoneVoice = jsonObject.getString("voiceCalcAmt")
                                        val phoneData = jsonObject.getString("dataCalcAmt")
                                        val phoneMms = jsonObject.getString("smsCalcAmt")

                                        views.setTextViewText(R.id.dataWidgetText, "${phoneData}") // 데이터
                                        views.setTextViewText(R.id.voiceWidgetText, "${phoneVoice}") //음성
                                        views.setTextViewText(R.id.mmsWidgetText,"${phoneMms}")   // 문자
                                    }


                                }

                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                                }

                            })


                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                        }

                    })


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