package com.example.baytro.data.user

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException

class RoleTypeAdapter : TypeAdapter<Role>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Role?) {
        Gson().toJson(value, Role::class.java, out)
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Role? {
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        val jsonObject = Gson().fromJson<JsonObject>(`in`, JsonObject::class.java)

        val type = jsonObject.get("type")?.asString

        return when (type) {
            "Landlord" -> {
                Gson().fromJson(jsonObject, Role.Landlord::class.java)
            }
            "Tenant" -> {
                Gson().fromJson(jsonObject, Role.Tenant::class.java)
            }
            else -> {
                null
            }
        }
    }
}