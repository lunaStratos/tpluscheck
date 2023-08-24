package com.lunastratos.postofficemvno

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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


class MainActivity : AppCompatActivity() {

    val companyList = arrayOf("선택안함", "LGT", "KT", "SKT")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 개인정보 저장소
        val sharedPreference = getSharedPreferences("saveUserInfo", Context.MODE_PRIVATE)

        //id match
        val getId = sharedPreference.getString("id", "")
        val getPw = sharedPreference.getString("pw", "")


        // 모든게 로그인 되어있다면
        if(!getId.equals("") && !getPw.equals("")){

            //UX상 화면상으로 가져와야 함
            findViewById<EditText>(R.id.idTxt).setText(getId)
            findViewById<EditText>(R.id.pwTxt).setText(getPw)

            getUserInfo(getId!!, getPw!!) // 로그인후 데이터 가져오기
        }

        // Login 버튼
        findViewById<Button>(R.id.loginBtn).setOnClickListener {
            
            // 값 가져오기
            val id = findViewById<EditText>(R.id.idTxt).text.toString()
            val pw = findViewById<EditText>(R.id.pwTxt).text.toString()

            var checkInput : Boolean = true

            //에러매칭
            if(id.equals("")){
                toast("아이디를 입력해주세요")
                checkInput = false
            }
            if(pw.equals("")){
                toast("암호를 입력해주세요")
                checkInput = false
            }

            if(checkInput){
                // 유저가 입력한 id, pw를 쉐어드에 저장한다.
                val editor = sharedPreference.edit()
                editor.putString("id", id)
                editor.putString("pw", pw)
                editor.apply()

                getUserInfo(id, pw) // 로그인후 데이터 가져오기
            }

        }

    }

    /**
     * 유저의 실시간 사용량조회 가져오기
     * */
    fun getUserInfo(id :String, pw: String){

        //retrofit 설정
        val client = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .build()
        val retrofit = Retrofit.Builder().baseUrl("https://insmobile.co.kr/")
            .client(client) //OkHttpClient 연결
            .build()
        val service = retrofit.create(RetrofitInterface::class.java);

        // Login 작업
        service.setLoginPage(
            id, pw, "",""
        ).enqueue(object : Callback<ResponseBody>{

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
                                    // 각 데이터 파싱

                                    // 타이틀
                                    val phoneVoiceTitle = jsonObject.getString("voiceDedtAmt")
                                    val phoneDataTitle = jsonObject.getString("dataDedtAmt")
                                    val phoneMmsTitle =jsonObject.getString("smsDedtAmt")

                                    // 사용
                                    val phoneVoiceUse = jsonObject.getString("voiceUsedAmt")
                                    val phoneDataUse =  jsonObject.getString("dataUsedAmt")
                                    val phoneMmsUse = jsonObject.getString("smsUsedAmt")

                                    // 남음
                                    val phoneVoice = jsonObject.getString("voiceCalcAmt")
                                    val phoneData = jsonObject.getString("dataCalcAmt")
                                    val phoneMms = jsonObject.getString("smsCalcAmt")

                                    // 기타정보
                                    val phoneUserContract = jsonObject.getString("PROD_NM") // 요금제

                                    findViewById<TextView>(R.id.dataText).setText("${phoneDataTitle}: ${phoneDataUse} / ${phoneData}")
                                    findViewById<TextView>(R.id.voiceText).setText("${phoneVoiceTitle}: ${phoneVoiceUse} / ${phoneVoice}")
                                    findViewById<TextView>(R.id.mmsText).setText("${phoneMmsTitle}: ${phoneMmsUse} / ${phoneMms}")

                                    findViewById<TextView>(R.id.signatureText).setText("${phoneUserContract}")

                                }

                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    toast("에러가 발생하여 실패하였습니다.")
                                }

                            })


                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            toast("에러가 발생하여 실패하였습니다.")
                        }

                    })

                }

            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 통신 실패 (인터넷 끊킴, 예외 발생 등 시스템적인 이유)
                toast("에러가 발생하여 실패하였습니다.")
            }
        })
    }

    fun toast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
