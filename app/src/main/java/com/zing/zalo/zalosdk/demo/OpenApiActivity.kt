package com.zing.zalo.zalosdk.demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zing.zalo.zalosdk.kotlin.oauth.ZaloSDK
import com.zing.zalo.zalosdk.kotlin.openapi.ZaloOpenApi
import com.zing.zalo.zalosdk.kotlin.openapi.ZaloOpenApiCallback
import com.zing.zalo.zalosdk.kotlin.openapi.ZaloPluginCallback
import com.zing.zalo.zalosdk.kotlin.openapi.model.FeedData
import org.json.JSONObject

class OpenApiActivity : AppCompatActivity(), ZaloOpenApiCallback, ZaloPluginCallback {
    @SuppressLint("SetTextI18n")
    override fun onResult(
        isSuccess: Boolean,
        error_code: Int,
        message: String?,
        jsonData: String?
    ) {
        callBackTextView.text = "${message.toString()}\n\n${jsonData.toString()}"
    }

    override fun onResult(data: JSONObject?) {
        callBackTextView.text = data.toString()
    }

    private lateinit var getProfileButton: Button
    private lateinit var getFriendListUsedAppButton: Button
    private lateinit var getFriendListInvitableButton: Button
    private lateinit var inviteFriendUseAppButton: Button
    private lateinit var postToWallButton: Button
    private lateinit var sendMsgToFriendButton: Button
    private lateinit var sendMessageViaApp: Button
    private lateinit var sharePostViaApp: Button

    private lateinit var callBackTextView: TextView

    private lateinit var zaloSDKClone:ZaloSDK

    private lateinit var zaloOpenApi: ZaloOpenApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_api)
        bindUI()
        configureLogic()
        configureUI()
        bindViewsListener()


    }

    //#region private supportive method
    private fun bindUI() {
        getProfileButton = findViewById(R.id.get_profile_button)
        getFriendListUsedAppButton = findViewById(R.id.get_friend_list_used_app_button)
        getFriendListInvitableButton = findViewById(R.id.get_friend_list_invitable_button)
        inviteFriendUseAppButton = findViewById(R.id.invite_friend_use_app_button)
        postToWallButton = findViewById(R.id.post_to_wall_button)
        sendMsgToFriendButton = findViewById(R.id.send_message_to_friend_button)
        sendMessageViaApp = findViewById(R.id.send_message_via_app_button)
        sharePostViaApp = findViewById(R.id.share_post_via_app)

        callBackTextView = findViewById(R.id.callback_text_view)
    }

    private fun configureUI() {

    }

    private fun configureLogic() {
        zaloSDKClone = ZaloSDK(this)
        zaloOpenApi = ZaloOpenApi(
            this,
            zaloSDKClone.getOauthCode()
        )
    }

    private fun bindViewsListener() {
        getProfileButton.setOnClickListener {
            val fields = arrayOf("id", "birthday", "gender", "picture", "name")
//            ZaloOpenApi.getInstance().getProfile(fields, this)
            zaloOpenApi.getProfile(fields, this)
        }
        getFriendListUsedAppButton.setOnClickListener {
            val fields = arrayOf("id", "name", "gender", "picture")
            zaloOpenApi.getFriendListUsedApp(fields, 0, 999, this)
        }
        getFriendListInvitableButton.setOnClickListener {
            val fields = arrayOf("id", "name", "gender", "picture")
            zaloOpenApi.getFriendListInvitable(fields, 0, 999, this)
        }

        inviteFriendUseAppButton.setOnClickListener {
            val friendsList = arrayOf("")
            zaloOpenApi.inviteFriendUseApp(friendsList, "Hello!", this)

        }

        postToWallButton.setOnClickListener {
            zaloOpenApi.postToWall(
                "http://vnexpress.net",
                "http://vnexpress.net",
                this
            )
        }

        sendMsgToFriendButton.setOnClickListener {
            zaloOpenApi.sendMsgToFriend("1491696566623706686", "msg", "http://vnexpress.net", this)
        }

        sendMessageViaApp.setOnClickListener {
            zaloOpenApi.shareMessage(mockFeedData(), this)
        }

        sharePostViaApp.setOnClickListener {
            zaloOpenApi.shareFeed(mockFeedData(), this)
        }
    }

    private fun mockFeedData(): FeedData {
        val feed = FeedData()
        feed.msg = "Prefill message"
        feed.link = "https://news.zing.vn"
        feed.linkTitle = "Zing News"
        feed.linkSource = "https://news.zing.vn"
        feed.linkThumb =
            listOf("https://img.v3.news.zdn.vn/w660/Uploaded/xpcwvovb/2015_12_15/cua_kinh_2.jpg")
        return feed
    }
    //#endregion
}
