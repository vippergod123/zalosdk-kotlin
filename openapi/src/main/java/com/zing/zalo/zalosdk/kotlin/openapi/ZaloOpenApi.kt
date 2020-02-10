package com.zing.zalo.zalosdk.kotlin.openapi

import android.content.Context
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.servicemap.ServiceMapManager
import com.zing.zalo.zalosdk.kotlin.openapi.model.FeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.jetbrains.annotations.Nullable

class ZaloOpenApi(context: Context, oauthCode: String?){

    companion object {

        internal var isBroadcastRegistered = false
    }

    private var job = Job()
    var scope = CoroutineScope(Dispatchers.IO + job)
    var accessTokenHttpClient =
        HttpClient(ServiceMapManager.getInstance().urlFor(ServiceMapManager.KEY_URL_OAUTH))
    var httpClient =
        HttpClient(ServiceMapManager.getInstance().urlFor(ServiceMapManager.KEY_URL_GRAPH))

    internal var openApi: IZaloOpenApi =
        OpenApi(
            context,
            oauthCode,
            isBroadcastRegistered,
            httpClient,
            accessTokenHttpClient,
            scope
        )

    /**
     * Get Zalo user's profile
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/thong-tin-nguoi-dung-post-28
     * @param fields   : id, birthday, gender, picture, name ex: {"id", "birthday", "gender", "picture", "name"}
     * @param callback
     */
    fun getProfile(fields: Array<String>, @Nullable callback: ZaloOpenApiCallback) {
        openApi?.getProfile(fields, callback)
    }

    /**
     * Lấy danh sách tất cả bạn bè của người dùng đã sử dụng ứng dụng
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/danh-sach-ban-be-post-34
     * @param fields   : Hỗ trợ các field: id, name, picture, gender
     * @param position position
     * @param count    count
     * @param callback
     */
    fun getFriendListUsedApp(
        fields: Array<String>,
        position: Int,
        count: Int,
        @Nullable callback: ZaloOpenApiCallback
    ) {
        openApi?.getFriendListUsedApp(fields, position, count, callback)
    }


    /**
     * Lấy danh sách bạn bè chưa sử dụng ứng dụng và có thể nhắn tin mời sử dụng ứng dụng
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/danh-sach-ban-be-post-34
     *
     * @param position position
     * @param count    count
     * @param callback
     * @param fields   : Hỗ trợ các field: id, name, picture, gender
     */
    fun getFriendListInvitable(
        fields: Array<String>,
        position: Int,
        count: Int,
        @Nullable callback: ZaloOpenApiCallback
    ) {
        openApi?.getFriendListInvitable(fields, position, count, callback)
    }


    /**
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/moi-su-dung-ung-dung-post-41
     * @param friendId ex: {"friend-id1", "friend-id2", "friend-id3"}
     * @param message  String
     * @param callback ZaloOpenApiCallback
     */
    fun inviteFriendUseApp(
        friendId: Array<String>,
        message: String,
        @Nullable callback: ZaloOpenApiCallback
    ) {
        openApi?.inviteFriendUseApp(friendId, message, callback)
    }


    /**
     * Post a feed to wall
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/dang-bai-viet-post-39
     * @param link     String url link
     * @param msg      String msg
     * @param callback ZaloOpenApiCallback
     */
    fun postToWall(link: String, msg: String, @Nullable callback: ZaloOpenApiCallback) {
        openApi?.postToWall(link, msg, callback)
    }

    /**
     * Send message to friend
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/goi-tin-nhan-toi-ban-be-post-1183
     * @param friendId Friend ID
     * @param msg      String content message
     * @param link     Link
     * @param callback ZaloOpenApiCallback
     */

    fun sendMsgToFriend(
        friendId: String,
        msg: String,
        link: String,
        @Nullable callback: ZaloOpenApiCallback
    ) {
        openApi?.sendMsgToFriend(friendId, msg, link, callback)
    }

    /**
     * gửi tin nhắn đến bạn bè thông qua app Zalo.
     * https://developers.zalo.me/docs/sdk/android-sdk/tuong-tac-voi-app-zalo/gui-tin-nhan-toi-ban-be-post-452
     * @param feedData: object feed cần share
     * @param callback: SDK sẽ gọi về callback này khi thông tin đã được gửicho app Zalo.
     */
    fun shareMessage(
        feedData: FeedData,
        callback: ZaloPluginCallback?
    ) {
        openApi?.shareMessage(feedData, callback)
    }

    /**
     * đăng bài viết lên trang nhật ký của user thông qua app Zalo.
     * https://developers.zalo.me/docs/sdk/android-sdk/tuong-tac-voi-app-zalo/dang-bai-viet-post-447
     * @param feedData: object feed cần share
     * @param callback: callback: SDK sẽ gọi về callback này khi thông tin đã được gửicho app Zalo.
     */
    fun shareFeed(
        feedData: FeedData,
        callback: ZaloPluginCallback?
    ) {
        openApi?.shareFeed(feedData, callback)
    }


}