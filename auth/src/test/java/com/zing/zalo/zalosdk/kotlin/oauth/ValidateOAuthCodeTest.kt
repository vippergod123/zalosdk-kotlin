package com.zing.zalo.zalosdk.kotlin.oauth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpUrlEncodedRequest
import com.zing.zalo.zalosdk.kotlin.oauth.callback.ValidateOAuthCodeCallback
import com.zing.zalo.zalosdk.kotlin.oauth.helper.AppInfoHelper
import com.zing.zalo.zalosdk.kotlin.oauth.helper.AuthStorage
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class ValidateOAuthCodeTest {
    private val resultAuth = JSONObject(
        """{
        "data":{
            "msg":"The code is still valid",
            "uid":12345,
            "expires_in":1568446493935
        },"error":0
    }"""
    )


    private lateinit var context: Context

    @MockK
    private lateinit var httpClient: HttpClient

    @MockK
    private lateinit var storage: AuthStorage

    private lateinit var sut: Authenticator

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()
        sut = Authenticator(context, storage)
        sut.httpClient = httpClient
        AppInfoHelper.setup()
    }

    @Test
    fun `validate auth code success`() {
        //1. setup
        every { storage.getZaloId() } returns 12345
        every { storage.getOAuthCode() } returns "abc"

        val request = slot<HttpUrlEncodedRequest>()
        every { httpClient.send(capture(request)).getJSON() } returns resultAuth

        //2 run
        sut.isAuthenticate("abc", object :
            ValidateOAuthCodeCallback {
            override fun onValidateComplete(
                validated: Boolean,
                errorCode: Int,
                userId: Long,
                authCode: String?
            ) {
                //3. verify
                assertThat(validated).isTrue()
                assertThat(errorCode).isEqualTo(0)
                assertThat(userId).isEqualTo(12345)
                assertThat(authCode).isEqualTo("abc")

                assertThat(request.captured.getUrl(""))
                    .isEqualTo("/v2/mobile/validate_oauth_code")

                val outputStream = ByteArrayOutputStream()
                request.captured.encodeBody(outputStream)
                val byteArray = outputStream.toByteArray()
                val body = String(byteArray)
                assertThat(body).isEqualTo(
                    "app_id=${AppInfoHelper.appId}" +
                            "&code=abc&version=${Constant.VERSION}&frm=sdk"
                )

            }

        })

        TestUtils.waitTaskRunInBackgroundAndForeground()
    }
}