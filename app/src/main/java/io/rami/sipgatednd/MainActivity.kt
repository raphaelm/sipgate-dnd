package io.rami.sipgatednd

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.extensions.authenticate
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.Result
import com.google.android.material.switchmaterial.SwitchMaterial
import io.rami.sipgatednd.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject


operator fun JSONArray.iterator(): Iterator<JSONObject> =
    (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()


class MainActivity : AppCompatActivity() {
    val active_groups = emptySet<String>().toMutableSet()
    private lateinit var binding: ActivityMainBinding
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.button.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        binding.switch1.setOnCheckedChangeListener { buttonView, isChecked ->
            disableAll()
            val data = JSONObject()
            data.put("dnd", isChecked)
            val req = "https://api.sipgate.com/v2/devices/${sp.getString("deviceid", "")}"
                .httpPut()
                .body(data.toString())
                .authenticate(sp.getString("username", "")!!, sp.getString("password", "")!!)
            req.headers["Content-Type"] = "application/json"
            req.responseString { request, response, result -> handleResult(result) }
        }
    }

    override fun onResume() {
        super.onResume()

        load()
    }

    fun enableAll() {
        binding.switch1.isEnabled = true
        val groups_ll = findViewById<LinearLayout>(R.id.groups)
        groups_ll.forEach {
            it.isEnabled = true
        }
        val lines_ll = findViewById<LinearLayout>(R.id.lines)
        lines_ll.forEach {
            it.isEnabled = true
        }
    }

    fun disableAll() {
        binding.switch1.isEnabled = false
        val groups_ll = findViewById<LinearLayout>(R.id.groups)
        groups_ll.forEach {
            it.isEnabled = false
        }
        val lines_ll = findViewById<LinearLayout>(R.id.lines)
        lines_ll.forEach {
            it.isEnabled = false
        }
    }

    fun load() {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        scope.launch(Dispatchers.IO) {
            "https://api.sipgate.com/v2/devices/${sp.getString("deviceid", "")}"
                .httpGet()
                .authentication().basic(sp.getString("username", "")!!, sp.getString("password", "")!!)
                .responseString() { request, response, result ->
                    when (result) {
                        is Result.Failure -> {
                            val ex = result.getException()
                            Toast.makeText(this@MainActivity, ex.toString(), Toast.LENGTH_SHORT)
                                .show()
                        }

                        is Result.Success -> {
                            scope.launch(Dispatchers.Main) {
                                val data = JSONObject(result.get())
                                active_groups.clear()
                                for (group in data.getJSONArray("activeGroups")) {
                                    active_groups.add(group.getString("id"))
                                }
                                for (group in data.getJSONArray("activePhonelines")) {
                                    active_groups.add(group.getString("id"))
                                }
                                binding.switch1.isChecked = data.getBoolean("dnd")
                            }
                            loadGroups()
                        }
                    }
                }
        }
    }

    fun loadGroups() {
        val groups_ll = findViewById<LinearLayout>(R.id.groups)
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        scope.launch(Dispatchers.IO) {
            "https://api.sipgate.com/v2/groups"
                .httpGet()
                .authentication().basic(sp.getString("username", "")!!, sp.getString("password", "")!!)
                .responseString() { request, response, result ->
                    when (result) {
                        is Result.Failure -> {
                            val ex = result.getException()
                            Toast.makeText(this@MainActivity, ex.toString(), Toast.LENGTH_SHORT)
                                .show()
                        }

                        is Result.Success -> {
                            scope.launch(Dispatchers.Main) {
                                groups_ll.removeAllViews()
                                val data = JSONObject(result.get())
                                for (item in data.getJSONArray("items")) {
                                    groups_ll.addView(switch(item, "groups"))
                                }
                                loadLines()
                            }
                        }
                    }
                }
        }
    }

    fun loadLines() {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val lines_ll = findViewById<LinearLayout>(R.id.lines)
        scope.launch(Dispatchers.IO) {
            "https://api.sipgate.com/v2/${sp.getString("userid", "w0")}/phonelines"
                .httpGet()
                .authentication().basic(sp.getString("username", "")!!, sp.getString("password", "")!!)
                .responseString() { request, response, result ->
                    when (result) {
                        is Result.Failure -> {
                            val ex = result.getException()
                            Toast.makeText(this@MainActivity, ex.toString(), Toast.LENGTH_SHORT)
                                .show()
                        }

                        is Result.Success -> {
                            scope.launch(Dispatchers.Main) {
                                lines_ll.removeAllViews()
                                val data = JSONObject(result.get())
                                for (item in data.getJSONArray("items")) {
                                    lines_ll.addView(switch(item, "phonelines"))
                                }
                                enableAll()
                            }
                        }
                    }
                }
        }
    }

    fun switch(item: JSONObject, type: String): SwitchMaterial {
        val newSwitch = SwitchMaterial(this@MainActivity)
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(30, 30, 30, 30)
        newSwitch.layoutParams = layoutParams
        newSwitch.text = item.getString("alias")
        newSwitch.isChecked = active_groups.contains(item.getString("id"))

        var prefix = type
        if (prefix == "phonelines") {
            prefix = sp.getString("userid", "w0") + "/phonelines"
        }
        newSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            disableAll()
            if (isChecked) {
                val data = JSONObject()
                data.put("deviceId", sp.getString("deviceid", ""))
                val req = "https://api.sipgate.com/v2/${prefix}/${item.getString("id")}/devices"
                    .httpPost()
                    .body(data.toString())
                    .authenticate(sp.getString("username", "")!!, sp.getString("password", "")!!)
                req.headers["Content-Type"] = "application/json"
                req.responseString { request, response, result -> handleResult(result) }
            } else {
                val req = "https://api.sipgate.com/v2/${prefix}/${item.getString("id")}/devices/${
                    sp.getString(
                        "deviceid",
                        ""
                    )
                }"
                    .httpDelete()
                    .authenticate(sp.getString("username", "")!!, sp.getString("password", "")!!)
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
                Toast.makeText(this@MainActivity, ex.toString(), Toast.LENGTH_SHORT).show()
                load()
            }

            is Result.Success -> {
                Toast.makeText(this@MainActivity, "OK!", Toast.LENGTH_SHORT).show()
                load()
            }
        }
    }
}
