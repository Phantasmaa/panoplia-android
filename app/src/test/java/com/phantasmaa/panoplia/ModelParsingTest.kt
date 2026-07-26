package com.phantasmaa.panoplia

import com.phantasmaa.panoplia.data.model.LoginResponse
import com.phantasmaa.panoplia.data.model.ServiceInfo
import com.phantasmaa.panoplia.data.model.User
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelParsingTest {

    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test
    fun `parses successful login response`() {
        val json = """{"ok":true,"user":{"id":1,"username":"Manuel","is_admin":true}}"""
        val adapter = moshi.adapter(LoginResponse::class.java)
        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals(true, parsed!!.ok)
        assertEquals("Manuel", parsed.user?.username)
        assertEquals(true, parsed.user?.isAdmin)
    }

    @Test
    fun `parses failed login response`() {
        val json = """{"ok":false,"error":"credenciales invalidas"}"""
        val adapter = moshi.adapter(LoginResponse::class.java)
        val parsed = adapter.fromJson(json)
        assertEquals(false, parsed!!.ok)
        assertEquals("credenciales invalidas", parsed.error)
    }

    @Test
    fun `parses list of services`() {
        val type = Types.newParameterizedType(List::class.java, ServiceInfo::class.java)
        val adapter = moshi.adapter<List<ServiceInfo>>(type)
        val sample = """[{"id":"x","name":"X","description":"d","url":"/x","icon":"🖼️","native":true}]"""
        val list = adapter.fromJson(sample)
        assertEquals(1, list?.size)
        assertEquals("x", list!![0].id)
        assertEquals(true, list[0].native)
    }
}
