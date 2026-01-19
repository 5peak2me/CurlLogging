package io.github.speak2me.kmp.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {

    private val httpClient = getHttpClient()

    fun httpDELETE() {
        viewModelScope.launch {
            httpClient.delete("/delete")
        }
    }

    fun httpGET() {
        viewModelScope.launch {
            httpClient.get("/get")
        }
    }

    fun httpPATCH() {
        viewModelScope.launch {
            httpClient.patch("/patch") {
                setBody("""{"key": "value"}""")
            }
        }
    }

    fun httpPOST() {
        viewModelScope.launch {
            httpClient.post("/post") {
                setBody("""{"key": "value"}""")
            }
        }
    }

    fun httpPUT() {
        viewModelScope.launch {
            httpClient.put("/put") {
                setBody("""{"key": "value"}""")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }
}