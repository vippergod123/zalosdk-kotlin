package com.zing.zalo.zalosdk.kotlin.openapi.model

data class FeedData(
    var msg: String,
    var linkTitle: String,
    var linkSource: String,
    var linkThumb: List<String>,
    var linkDesc: String,
    var link: String
) {
    constructor() : this("", "", "", emptyList<String>(), "", "")
}