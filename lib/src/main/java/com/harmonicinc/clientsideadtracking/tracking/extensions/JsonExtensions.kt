package com.harmonicinc.clientsideadtracking.tracking.extensions

import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration

fun JSONObject.toMap(): Map<String, *> = keys().asSequence().associateWith {
    when (val value = this[it])
    {
        is JSONArray ->
        {
            val map = (0 until value.length()).associate { Pair(it.toString(), value[it]) }
            JSONObject(map).toMap().values.toList()
        }
        is JSONObject -> value.toMap()
        JSONObject.NULL -> null
        else            -> value
    }
}

fun JSONArray.toList(): List<Any> {
    val list = mutableListOf<Any>()
    for (i in 0 until this.length()) {
        var value: Any = this[i]
        when (value) {
            is JSONArray -> value = value.toList()
            is JSONObject -> value = value.toMap()
        }
        list.add(value)
    }
    return list
}

fun JSONObject.optIntNull(name: String): Int? {
    val v = this.optInt(name)
    return if (v == 0) null else v
}

fun JSONObject.optDuration(name: String): Long {
   /* Code block start
   Below commented code converts the string in the format "MM:SS" to a Duration object
   & further converts in to milliseconds.
   e.g: Input is 11:30 (11 minutes and 30 seconds)
   var v = this.optString(name) // v = "11:30"
    if (v.isEmpty()) return 0
    val arr: List<String> = v.split(":") // arr = [11, 30]
    var duration: Duration = Duration.ZERO // Default duration is zero "PT0S"
    if (arr.size == 2) {
        v = "PT" + arr[0] + "M" + arr[1] + "S" //v = PT11M30S
        duration = Duration.parse(v) // duration = 690 which equals to 11 minutes * 60 + 30 seconds
    }
    return duration.toMillis()  // duration.toMillis() = 690000 (in milliseconds)
    Code block end
    */
    //Above code uses Duration class which is available in Java 8 and above.
    // & does required API level 26 (Android 8.0) or above.
    // In order to support lower API levels, we will use below custom implementation.

    val v = this.optString(name)
    if (v.isEmpty()) return 0
    val parts: List<String> = v.split(":")
    if (parts.size != 2) return 0L

    val minutes = parts[0].toLongOrNull() ?: return 0L
    val seconds = parts[1].toLongOrNull() ?: return 0L

    return (minutes * 60 + seconds) * 1000
}