package io.rami.sipgatednd

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout
import android.widget.Switch
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.json.JSONArray
import org.json.JSONObject
import android.view.ViewGroup
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpPost
import org.jetbrains.anko.forEachChild


operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()


class MainActivity : AppCompatActivity() {
    val active_groups = emptySet<String>().toMutableSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        switch1.setOnCheckedChangeListener { buttonView, isChecked ->
            disableAll()
            val data = JSONObject()
            data.put("dnd", isChecked)
            val req = "https://api.sipgate.com/v2/devices/${defaultSharedPreferences.getString("deviceid", "")}"
                    .httpPut()
                    .body(data.toString())
                    .authenticate(defaultSharedPreferences.getString("username", ""), defaultSharedPreferences.getString("password", ""))
            req.headers["Content-Type"] = "application/json"
            req.responseString { request, response, result -> handleResult(result) }
        }
    }

    override fun onResume() {
        super.onResume()

        load()
    }

    fun enableAll() {
        switch1.isEnabled = true
        val groups_ll = findViewById<LinearLayout>(R.id.groups)
        groups_ll.forEachChild {
            it.isEnabled = true
        }
    }

    fun disableAll() {
        switch1.isEnabled = false
        val groups_ll = findViewById<LinearLayout>(R.id.groups)
        groups_ll.forEachChild {
            it.isEnabled = false
        }
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
                                    active_groups.clear()
                                    for (group in data.getJSONArray("activeGroups")) {
                                        active_groups.add(group.getString("id"))
                                    }
                                    switch1.isChecked = data.getBoolean("dnd")
                                }
                                loadGroups()
                            }
                        }
                    }
        }
    }

    fun loadGroups() {
        val groups_ll = findViewById<LinearLayout>(R.id.groups)
        "https://api.sipgate.com/v2/groups"
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
                                groups_ll.removeAllViews()
                                val data = JSONObject(result.get())
                                for (item in data.getJSONArray("items")) {
                                    groups_ll.addView(switch(item))
                                }
                                enableAll()
                            }
                        }
                    }
                }
    }

    fun switch(item: JSONObject): Switch {
        val newSwitch = Switch(this@MainActivity)
        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(30, 30, 30, 30)
        newSwitch.layoutParams = layoutParams
        newSwitch.text = item.getString("alias")
        newSwitch.tag = item.getString("id")
        newSwitch.isChecked = active_groups.contains(item.getString("id"))
        System.out.println("Group " + item.getString("alias") + ": " + newSwitch.isChecked)

        newSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            disableAll()
            if (isChecked) {
                val data = JSONObject()
                data.put("deviceId", defaultSharedPreferences.getString("deviceid", ""))
                val req = "https://api.sipgate.com/v2/groups/${item.getString("id")}/devices"
                        .httpPost()
                        .body(data.toString())
                        .authenticate(defaultSharedPreferences.getString("username", ""), defaultSharedPreferences.getString("password", ""))
                req.headers["Content-Type"] = "application/json"
                req.responseString { request, response, result -> handleResult(result) }
            } else {
                val req = "https://api.sipgate.com/v2/groups/${item.getString("id")}/devices/${defaultSharedPreferences.getString("deviceid", "")}"
                        .httpDelete()
                        .authenticate(defaultSharedPreferences.getString("username", ""), defaultSharedPreferences.getString("password", ""))
                req.headers["Content-Type"] = "application/json"
                req.responseString { request, response, result -> handleResult(result) }
            }
        }

        return newSwitch
    }

    fun handleResult(result: Result<String, FuelError>) {
        when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                toast(ex.toString())
                load()
            }
            is Result.Success -> {
                toast("OK!")
                load()
            }
        }
    }
}
