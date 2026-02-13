package com.example.mobilewebapp.bridge

import org.json.JSONObject

data class BridgeRequest(
    val id: String,
    val method: String,
    val params: JSONObject
) {
    companion object {
        fun fromJson(json: String): BridgeRequest {
            val obj = JSONObject(json)
            return BridgeRequest(
                id = obj.getString("id"),
                method = obj.getString("method"),
                params = obj.optJSONObject("params") ?: JSONObject()
            )
        }
    }
}

object BridgeResponse {
    fun success(id: String, data: JSONObject = JSONObject()): String {
        return JSONObject().apply {
            put("id", id)
            put("type", "response")
            put("success", true)
            put("data", data)
        }.toString()
    }

    fun failure(id: String, error: String): String {
        return JSONObject().apply {
            put("id", id)
            put("type", "response")
            put("success", false)
            put("error", error)
        }.toString()
    }
}

object BridgeEvent {
    fun create(event: String, data: JSONObject = JSONObject()): String {
        return JSONObject().apply {
            put("type", "event")
            put("event", event)
            put("data", data)
        }.toString()
    }
}
