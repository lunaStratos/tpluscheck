package com.lunastratos.postofficemvno

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
        val getSelectPhoneNumber = sharedPreference.getString("selectPhoneNumber", "")
        val carrierCompanyPosition = sharedPreference.getInt("carrierCompanyPosition", 0)

        //회사선택 스피너
        val spinner = findViewById<Spinner>(R.id.spn_SPList)
        spinner.adapter = ArrayAdapter.createFromResource(this, R.array.selectCarrierCompany, android.R.layout.simple_list_item_1).also {
            adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        // 회사선택 adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Log.d("===========", " " + position )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        // 모든게 로그인 되어있다면
        if(!getId.equals("") && !getPw.equals("") && !getSelectPhoneNumber.equals("") && carrierCompanyPosition != 0 ){

            //UX상 화면상으로 가져와야 함
            findViewById<EditText>(R.id.idTxt).setText(getId)
            findViewById<EditText>(R.id.pwTxt).setText(getPw)
            findViewById<EditText>(R.id.phoneNumberTxt).setText(getSelectPhoneNumber)
            spinner.setSelection(carrierCompanyPosition) //스피너 선택

            Log.d("carrierCompanyPosition", ""+ carrierCompanyPosition)
            val carrierCompanyStr : String = companyList[carrierCompanyPosition] // 회사명

            getUserInfo(getId!!, getPw!!, getSelectPhoneNumber!!, carrierCompanyStr!!) // 로그인후 데이터 가져오기
        }

        // Login 버튼
        findViewById<Button>(R.id.loginBtn).setOnClickListener {
            
            // 값 가져오기
            val id = findViewById<EditText>(R.id.idTxt).text.toString()
            val pw = findViewById<EditText>(R.id.pwTxt).text.toString()
            val selectPhoneNumber = findViewById<EditText>(R.id.phoneNumberTxt).text.toString()
            val carrierCompanyStr = spinner.selectedItem as String

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
            if(selectPhoneNumber.length !=13){
                toast("전화번호를 입력해주세요(-)포함, 예) 010-1234-1235")
                checkInput = false
            }
            if(carrierCompanyStr.equals("선택하세요")){
                toast("회사를 선택해주세요")
                checkInput = false
            }
            
            if(checkInput){
                // 유저가 입력한 id, pw를 쉐어드에 저장한다.
                val editor = sharedPreference.edit()
                editor.putString("id", id)
                editor.putString("pw", pw)
                editor.putString("selectPhoneNumber", selectPhoneNumber)
                editor.putInt("carrierCompanyPosition", spinner.selectedItemPosition)
                editor.apply()

                getUserInfo(id, pw, selectPhoneNumber, carrierCompanyStr) // 로그인후 데이터 가져오기
            }

        }

    }

    fun getUserInfo(id :String, pw: String, useNum: String, telecomId:String){

        //retrofit 설정
        val client = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .build()
        val retrofit = Retrofit.Builder().baseUrl("https://www.tplusmobile.com")
            .client(client) //OkHttpClient 연결
            .build()
        val service = retrofit.create(RetrofitInterface::class.java);

        // Login 작업
        service.setLoginPage(
            id, pw, "true"
        ).enqueue(object : Callback<ResponseBody>{

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if(response.isSuccessful){
                    // 정상적으로 통신이 성공된 경우
                    var result: String? = response.body()!!.string()
                    var code: String? = response.code().toString()
                    val docAfterLogin: Document = Jsoup.parse(result)

//                    Log.d("YMC", "onResponse 성공: " + result?.toString())
//                    Log.d("YMC", "onResponse 성공: " + code?.toString())
//                    Log.d("YMC", "onResponse 성공: " + docAfterLogin.select("li.u2").select("span.atxt").text())

                    //로그인 안됨
                    if(docAfterLogin.select("li.u2").select("span.atxt").text().equals("로그인")){
                        toast("로그인에 실패했습니다.\n아이디와 암호를 확인해주세요")
                    }

                    //로그인 됨
                    if(docAfterLogin.select("li.u21").select("span.atxt").text().equals("로그아웃")){
                        toast("로그인에 성공!\n잠시만 기다려 주세요")

                        //번호선택
                        service.getSelectPhoneNumberPage(useNum ,telecomId ).enqueue(object  : Callback<ResponseBody>{
                            override fun onResponse(
                                call2: Call<ResponseBody>,
                                getSelectPhoneNumberResponseBody: Response<ResponseBody>
                            ) {
                                var selectPhoneNumberHtml: String? = getSelectPhoneNumberResponseBody.body()!!.string()
                                val selectPhoneNumberDoc: Document = Jsoup.parse(selectPhoneNumberHtml)
                                //Log.d("getSelectPhoneNumberPage", " " + selectPhoneNumberDoc)
                                
                                // 마지막 결과 페이지
                                service.getDataPage().enqueue(object : Callback<ResponseBody>{
                                    override fun onResponse(
                                        call3: Call<ResponseBody>,
                                        dataPageResponse: Response<ResponseBody>
                                    ) {
                                        var html: String? = dataPageResponse.body()!!.string()
                                        val doc: Document = Jsoup.parse(html)

                                        //Log.d("getDataPage", " getDataPage=>  " + doc.select("div.voice"))

                                        // 각 데이터 파싱

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

                                        // 기타정보
                                        val phoneUserName = doc.select("div.box.box1").select("strong").text() // 사용자이름
                                        val phoneUserContract = doc.select("div.box.box2").select("em").text() // 요금제
                                        val phoneUserNumber = doc.select("p.mt10").text() // 번호

                                        //
                                        val textArray = arrayOf("데이터", "음성", "문자")
                                        val textTitleArray = arrayOf(
                                           "${phoneDataTitle},${phoneDataLimit},${phoneData}",
                                            "${phoneVoiceTitle},${phoneVoiceLimit},${phoneVoice}",
                                            "${phoneMmsTitle},${phoneMmsLimit},${phoneMms}"
                                        )

                                        // 조합
                                        // LGT는 voice가 데이터인 상황, SKT는 음성으로 표시됨
                                        // 랜덤이라 직접 찾아야 함
                                        for ((index, item )in textArray.withIndex()){
                                            for ((titleIndex, titleItem) in textTitleArray.withIndex()){

                                                if(titleItem.indexOf(item) != -1){//일치하는게 있다면
                                                    Log.d("phoneMmsTitle",  "${index} / ${item} , ${titleItem}")
                                                    val titleItemArray = titleItem.split(",")
                                                    when (index){
                                                        0 -> { // 타이틀텍스트에 [데이터]가 들어간 경우
                                                            if(titleItem.indexOf("GB") != -1){
                                                                val dataLimit = (titleItemArray[1].toDouble()*1024).toInt()
                                                                val dataUse = (titleItemArray[2].toDouble()*1024).toInt()
                                                                findViewById<TextView>(R.id.dataText).setText("${titleItemArray[0]}: ${dataLimit} / ${dataUse}")
                                                            }
                                                            if(titleItem.indexOf("MB") != -1) findViewById<TextView>(R.id.dataText).setText("${titleItemArray[0]}: ${titleItemArray[1]} / ${titleItemArray[2]}")

                                                        }
                                                        1 -> { // 타이틀텍스트에 [음성]이 들어간 경우
                                                            findViewById<TextView>(R.id.voiceText).setText("${titleItemArray[0]}: ${titleItemArray[1]} / ${titleItemArray[2]}")
                                                        }
                                                        2 -> { // 타이틀텍스트에 [문자]가 들어간 경우
                                                            findViewById<TextView>(R.id.mmsText).setText("${titleItemArray[0]}: ${titleItemArray[1]} / ${titleItemArray[2]}")
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        findViewById<TextView>(R.id.signatureText).setText("${phoneUserContract}")
                                        findViewById<TextView>(R.id.userText).setText("${phoneUserName} / ${phoneUserNumber}")

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



                }else{
                    // 통신이 실패한 경우(응답코드 3xx, 4xx 등)
                    toast("에러가 발생하여 실패하였습니다.")
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
