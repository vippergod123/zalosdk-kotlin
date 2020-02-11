package com.zing.zalo.zalosdk.kotlin.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.zalosdk.kotlin.core.http.HttpGetRequest
import com.zing.zalo.zalosdk.kotlin.core.http.HttpMultipartRequest
import com.zing.zalo.zalosdk.kotlin.core.http.HttpMultipartRequest.Companion.BOUNDARY
import com.zing.zalo.zalosdk.kotlin.core.http.HttpResponse
import com.zing.zalo.zalosdk.kotlin.core.http.HttpUrlEncodedRequest
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class HttpTest
{
     private lateinit var context: Context

     @Before
     fun setup()
     {
          MockKAnnotations.init(this, relaxUnitFun = true)
          context = ApplicationProvider.getApplicationContext()
     }

     private fun testGet(url: String, path: String, expectUrl: String) {
          val sut = HttpGetRequest(path)

          //test headers
          sut.addHeader("header1", "value1")
          sut.addHeader("header2", "value2")

          val headers = sut.getHeaders()
          assertThat(headers["header1"]).isEqualTo("value1")
          assertThat(headers["header2"]).isEqualTo("value2")
          assertThat(headers["SDK-Source"]).isEqualTo("ZaloSDK-${Constant.VERSION}")
          assertThat(headers["Accept-Charset"]).isEqualTo("UTF-8")

          sut.addQueryStringParameter("param1", "pv1")
          sut.addQueryStringParameter("param2", "pv2")
          sut.addQueryStringParameters(mapOf(
               "param3" to "pv3", "param4" to "pv4"
          ))
          assertThat(sut.getUrl(url)).isEqualTo(expectUrl)
     }

     @Test
     fun `http get request`() {
          val url = "http://abc.com"
          val path = "/a/b/c"
          val params = "param1=pv1&param2=pv2&param3=pv3&param4=pv4"
          testGet(url, path, "$url$path?$params")
          testGet(url, "$path?a=b", "$url$path?a=b&$params")
          testGet(url, "$path?a=b&c=d", "$url$path?a=b&c=d&$params")
     }

     @Test
     fun `http post url encoded request`() {
          val url = "http://abc.com"
          val path = "/a/b/c"

          val sut = HttpUrlEncodedRequest(path)
          sut.addParameter("param1", "pv1")
          sut.addParameter("param2", "pv2")
          sut.addParameters(mapOf(
               "param3" to "pv3", "param4" to "pv4"
          ))

          val outputStream = ByteArrayOutputStream()
          sut.encodeBody(outputStream)
          val byteArray = outputStream.toByteArray()
          val body = String(byteArray)

          assertThat(sut.getHeaders()["Content-Type"]).isEqualTo("application/x-www-form-urlencoded;charset=UTF-8")
          assertThat(sut.getUrl(url)).isEqualTo("$url$path")
          assertThat(body).isEqualTo("param1=pv1&param2=pv2&param3=pv3&param4=pv4")
     }

     @Test
     fun `http post multipart form request`() {
          val url = "http://abc.com"
          val path = "/a/b/c"

          val sut = HttpMultipartRequest(path)
          sut.addParameter("param1", "pv1")
          sut.setFileParameter("fileKey", "fileName", "fileData".toByteArray())

          val outputStream = ByteArrayOutputStream()
          sut.encodeBody(outputStream)
          val byteArray = outputStream.toByteArray()
          val body = String(byteArray)

          val headers = sut.getHeaders()
          assertThat(headers["Content-Type"]).isEqualTo("multipart/form-data;boundary=${BOUNDARY}")
          assertThat(headers["Connection"]).isEqualTo("Keep-Alive")
          assertThat(headers["ENCTYPE"]).isEqualTo("multipart/form-data")
          assertThat(headers["uploaded_file"]).isEqualTo("fileName")

          val expected = """--*****
Content-Disposition: form-data; name="param1"
Content-Type: text/plain; charset=UTF-8

pv1
--*****
Content-Disposition: form-data; name=fileKey; filename=fileName

fileData
--*****--

""".trimIndent().replace("\n", "\r\n")
          assertThat(body).isEqualTo(expected)
     }

     @Test
     fun `http text response`() {
          val data = "abc"
          val inputStream = ByteArrayInputStream(data.toByteArray())
          val request = mockk<HttpGetRequest>()
          val sut = HttpResponse(request)
          sut.responseCode = 200
          sut.responseStream = inputStream

          assertThat(request).isEqualTo(sut.request)
          assertThat(sut.getStatusCode()).isEqualTo(200)
          assertThat(sut.getText()).isEqualTo(data)
     }

     @Test
     fun `http json response`() {
          val data = """{ "abc": "def" }"""
          val inputStream = ByteArrayInputStream(data.toByteArray())
          val request = mockk<HttpGetRequest>()
          val sut = HttpResponse(request)
          sut.responseStream = inputStream

          assertThat(sut.getJSON()!!.getString("abc")).isEqualTo("def")
     }
}