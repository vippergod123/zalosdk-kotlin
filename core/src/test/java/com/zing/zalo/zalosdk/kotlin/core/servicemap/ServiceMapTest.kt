package com.zing.zalo.zalosdk.kotlin.core.servicemap

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.helper.DataHelper
import com.zing.zalo.zalosdk.kotlin.core.helper.TestUtils
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpGetRequest
import com.zing.zalo.zalosdk.kotlin.core.http.HttpResponse
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ServiceMapTest {
    private val RESULT =
        "4CB543CAD8B1FBE8030AFE850F1F27FD01ACD1F05890C5E79C1D722C3E3AFCF20BB0C2D9804FF841AFBF481A6C3319B19D31C69E63D0FC734EBE1E94B63C4176BB7BBE1A74A480A5255FF8A59B9D623A18D757D0A2A8065A24E4D7E32EEDC77A40F93DFD803CFC52683CADBC95D72B303FEA503CF32CA1566132594F21D789B4FD026C0901E1E8566C084BEF5D14EE14A287BD42961FBA69D6AC5B8E020755BE5BF97FFE21D511D25033F51D01476ABFE040CCB724706417E2FFBF51A2DD6B030EA6E1C9BFF4326014ECC0F7208BB227318BFD6DE8EA6D3DA53A2B79BE668533C5AD05C5D978511B68B5ED6DF72BA20DEF638AA1B0C4ADB528DAC14DD11FC925CC16478339CCC2DAC63A6CC6C27B63DD40B24CA8B195B5143D7C3BE40083B712CBF5A31A8E3FDF1618D06160F57124DABDBBE82FAD860D9F8FE3B7CEA15A950026842C183F0D1CD47319AE7900817012347917BDF68ED69363B9C2252E20BCC705190981AC00078E561F77A7F7E28D62371AABE20DEEB807D7FF3BD175BFDBD398FB11D47C73520B6F9E03008CD5E874E1E5C71C5D3C55403655A3DC62C1C8B678FC74F94C5308A3C67F1DA35CDCBC976F65F5FE5E31D7FF87C631D794CBD60504EC65950602DAE1227EB024B182702754A0EDE2B65EE7BCC1464057FB7DE515FEF70EFC4BEC861AAE6E2E52358AA2FF675567ACB892F9C1550F80C7D84ACA0A870971930D22180BDCA3C4B69554CC6A8AE2A7E0C2786A4381C250232966F94F09A445F8CC4BBE43214143AF383B78A92B5E7CFB603398EC46C92C956874E2AAFB98799E054D5F84E75D6DFB280F94A37F68BC7F51BDADA690D59ED294164CC2AE88B36A725EF7AB3CBAE9B42B071AA534819BC080727943EC1B6FF39CD3F4BEFA48AE28B4AF694A54106E056D16EE8947EA9CA38813BFBA6C1EDEF3BB9E88A6CBA9EA79E2C711A290886FA13C3740EE"

    private val SERVICE_MAP_URLS = arrayOf(
        "https://mp3.zing.vn/zdl/service_map_all.bin",
        "https://zaloapp.com/zdl/service_map_all.bin",
        "https://news.zing.vn/zdl/service_map_all.bin",
        "https://n.zing.vn/zdl/service_map_all.bin",
        "https://srv.mp3.zing.vn/zdl/service_map_all.bin"
    )

    private lateinit var context: Context

    @MockK private lateinit var response1: HttpResponse
    @MockK private lateinit var response2: HttpResponse
    @MockK private lateinit var storage: ServiceMapStorage
    @MockK private lateinit var client: HttpClient
    private lateinit var sut: ServiceMapManager

    private val testScope = TestCoroutineScope()

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        context = ApplicationProvider.getApplicationContext()
        sut = ServiceMapManager.getInstance()
        sut.httpClient = client
        sut.storage = storage

        val serviceMapData = DataHelper.serviceMap
        every { storage.getKeyUrlCentralized() } returns serviceMapData.URL_CENTRALIZED
        every { storage.getKeyUrlOauth() } returns serviceMapData.URL_OAUTH
        every { storage.getKeyUrlGraph() } returns serviceMapData.URL_GRAPH
    }

    @After
    fun teardown() {
        sut.stop()
    }

    @Test
    fun noLoadIfCached() {
        //1. mock
        every { storage.getExpireTime() } returns System.currentTimeMillis() + 1000 * 60 * 59 * 24

        //2. run
        sut.scope = testScope
        sut.start(context)

        TestUtils.waitTaskRunInBackgroundAndForeground()

        //3. verify
        verify(exactly = 0) { client.send(any()).getText() }
    }

    @Test
    fun downloadServiceMapTestInDevMode() {
        //1. mock
        Constant.DEV_MODE = true
        every { storage.getExpireTime() } returns 0L
        val requests = mutableListOf<HttpGetRequest>()

        every { client.send(capture(requests)) } returns response1 andThen response2

        every { response1.getText() } returns "ABC"
        every { response2.getText() } returns RESULT
        every { storage.getExpireTime() } returns 0L

        //2. run
        sut.scope = testScope
        sut.start(context)

        //3. verify
        //3.a load from cache
        verifyLoadFromCache(1)

        //3.b save to cache
        val WEB_LOGIN_PATH = "/v3/auth?app_id="

        val urlOauth = "https://oauth.zaloapp.com/v3/auth?app_id="
        val urlGraph = "https://graph.zaloapp.com/v3/auth?app_id="
        val urlCentralized = "https://centralized.zaloapp.com/v3/auth?app_id="
        Thread.sleep(1000)

        val testUrlOauth = ServiceMapManager.getInstance().urlFor(ServiceMapManager.KEY_URL_OAUTH, WEB_LOGIN_PATH)
        val testUrlGraph = ServiceMapManager.getInstance().urlFor(ServiceMapManager.KEY_URL_GRAPH, WEB_LOGIN_PATH)
        val testUrlCentralized =
            ServiceMapManager.getInstance().urlFor(ServiceMapManager.KEY_URL_CENTRALIZED, WEB_LOGIN_PATH)

        //3.c return results
        assertThat(urlOauth).isEqualTo(testUrlOauth)
        assertThat(urlGraph).isEqualTo(testUrlGraph)
        assertThat(urlCentralized).isEqualTo(testUrlCentralized)

        assertThat(requests.size).isEqualTo(0)
    }


    @Test
    fun `download ServiceMap Test Live Mode`() = runBlockingTest {
        //1. mock
        Constant.DEV_MODE = false
        every { storage.getExpireTime() } returns 0L
        val requests = mutableListOf<HttpGetRequest>()

        every { client.send(capture(requests)) } returns response1 andThen response2

        every { response1.getText() } returns "ABC"
        every { response2.getText() } returns RESULT
        every { storage.getExpireTime() } returns 0L

        //2. run
        sut.scope = testScope
        sut.start(context)

        //3. verify
        //3.a load from cache
        verifyLoadFromCache(1)
        Thread.sleep(1000)
        //3.b save to cache
        verifySaveCache(1)

        val WEB_LOGIN_PATH = "/v3/auth?app_id="

        val urlOauth = "https://oauth.zaloapp.com/v3/auth?app_id="
        val urlGraph = "https://graph.zaloapp.com/v3/auth?app_id="
        val urlCentralized = "https://centralized.zaloapp.com/v3/auth?app_id="

        val testUrlOauth =
            ServiceMapManager.getInstance().urlFor(ServiceMapManager.KEY_URL_OAUTH, WEB_LOGIN_PATH)
        val testUrlGraph =
            ServiceMapManager.getInstance().urlFor(ServiceMapManager.KEY_URL_GRAPH, WEB_LOGIN_PATH)
        val testUrlCentralized =
            ServiceMapManager.getInstance()
                .urlFor(ServiceMapManager.KEY_URL_CENTRALIZED, WEB_LOGIN_PATH)

        //3.c return results
        assertThat(urlOauth).isEqualTo(testUrlOauth)
        assertThat(urlGraph).isEqualTo(testUrlGraph)
        assertThat(urlCentralized).isEqualTo(testUrlCentralized)

        assertThat(requests.size).isEqualTo(2)
        assertThat(requests[0].getUrl("")).isEqualTo(SERVICE_MAP_URLS[0])
        assertThat(requests[1].getUrl("")).isEqualTo(SERVICE_MAP_URLS[1])
    }

    private fun verifySaveCache(times:Int ) {
        verify(exactly = times) { storage.setKeyUrlCentralized("https://centralized.zaloapp.com") }
        verify(exactly = times) { storage.setKeyUrlGraph("https://graph.zaloapp.com") }
        verify(exactly = times) { storage.setKeyUrlOauth("https://oauth.zaloapp.com") }
    }

    private fun verifyLoadFromCache(times:Int ) {
        verify(exactly = times) { storage.getKeyUrlCentralized() }
        verify(exactly = times) { storage.getKeyUrlGraph() }
        verify(exactly = times) { storage.getKeyUrlOauth() }
    }
}