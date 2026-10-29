package com.example.smarthomeai.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Lưu lịch sử, cảnh báo, trạng thái thiết bị và cài đặt trong SharedPreferences. */
class Storage(context: Context) {

    private val prefs = context.getSharedPreferences("smart_home", Context.MODE_PRIVATE)

    fun load(): SmartHomeState {
        val d = SmartHomeState()
        return d.copy(
            light = prefs.getBoolean("light", d.light),
            doorOpen = prefs.getBoolean("door", d.doorOpen),
            alarm = prefs.getBoolean("alarm", d.alarm),
            esp32Ip = prefs.getString("ip", d.esp32Ip) ?: d.esp32Ip,
            autoAlarm = prefs.getBoolean("auto_alarm", d.autoAlarm),
            notifyEnabled = prefs.getBoolean("notify", d.notifyEnabled),
            logs = readList("logs") { LogItem(it.getString("k"), it.getString("t"), it.getString("tm")) },
            alerts = readList("alerts") {
                AlertItem(
                    runCatching { AlertType.valueOf(it.getString("k")) }.getOrDefault(AlertType.MOTION),
                    it.getString("t"),
                    it.getString("tm")
                )
            },
            faces = readList("faces") { FaceRecord(it.getString("n"), it.getString("tm"), it.getBoolean("known")) }
        )
    }

    fun save(s: SmartHomeState) {
        prefs.edit()
            .putBoolean("light", s.light)
            .putBoolean("door", s.doorOpen)
            .putBoolean("alarm", s.alarm)
            .putString("ip", s.esp32Ip)
            .putBoolean("auto_alarm", s.autoAlarm)
            .putBoolean("notify", s.notifyEnabled)
            .putString("logs", toJson(s.logs) { JSONObject().put("k", it.kind).put("t", it.title).put("tm", it.time) })
            .putString("alerts", toJson(s.alerts) { JSONObject().put("k", it.type.name).put("t", it.title).put("tm", it.time) })
            .putString("faces", toJson(s.faces) { JSONObject().put("n", it.name).put("tm", it.time).put("known", it.known) })
            .apply()
    }

    private fun <T> toJson(list: List<T>, build: (T) -> JSONObject): String =
        JSONArray().also { arr -> list.forEach { arr.put(build(it)) } }.toString()

    private fun <T> readList(key: String, parse: (JSONObject) -> T): List<T> = try {
        val arr = JSONArray(prefs.getString(key, "[]"))
        List(arr.length()) { parse(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }
}
