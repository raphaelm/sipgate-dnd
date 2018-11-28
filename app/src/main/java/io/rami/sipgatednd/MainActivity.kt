package io.rami.sipgatednd

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        switch1.setOnCheckedChangeListener { buttonView, isChecked ->
            val data = JSONObject()
            data.put("dnd", isChecked)
            val req = "https://api.sipgate.com/v2/devices/${defaultSharedPreferences.getString("deviceid", "")}"
                    .httpPut()
                    .body(data.toString())
                    .authenticate(defaultSharedPreferences.getString("username", ""), defaultSharedPreferences.getString("password", ""))
            req.headers["Content-Type"] = "application/json"
            req.responseString() { request, response, result ->
                when (result) {
                    is Result.Failure -> {
                        val ex = result.getException()
                        toast(ex.toString())
                    }
                    is Result.Success -> {
                        toast("OK!")
                        load()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        load()
    }

    fun load() {
        doAsync {
            "https://api.sipgate.com/v2/devices/${defaultSharedPreferences.getString("deviceid", "")}"
                    .httpGet()
                    .authenticate(defaultSharedPreferences.getString("username", ""), defaultSharedPreferences.getString("password", ""))
                    .responseString() { request, response, result ->
                        when (result) {
                            is Result.Failure -> {
                                val ex = result.getException()
                                toast(ex.toString())
                            }
                            is Result.Success -> {
                                runOnUiThread {
                                    val data = JSONObject(result.get())
                                    switch1.isChecked = data.getBoolean("dnd")
                                    switch1.isEnabled = true
                                }
                            }
                        }
                    }
        }
    }
}
