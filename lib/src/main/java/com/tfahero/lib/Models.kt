package com.tfahero.lib

import com.google.gson.annotations.SerializedName

const val CODE_SUCCESS = 105
const val INSUFFICIENT_FUNDS = 104
const val CODE_NOT_MATCHED = 101
const val CODE_RATE_LIMIT_REACHED = 103
const val CODE_EXPIRED = 102
const val CODE_NOT_FOUND = 100

data class Send2faRequest(
    @SerializedName("phone")
    val phone: String
)

data class Send2faResponse(
    @SerializedName("code_identifier")
    val codeIdentifier: String = ""
)

data class Verify2faRequest(
    @SerializedName("code")
    val code: String,
    @SerializedName("code_identifier")
    val codeIdentifier: String
)

data class Verify2faResponse(
    val code: Int,
    val message: String
)

class HttpResponse<T> {
    val error: Boolean = false
    val message: String = ""
    val data: T? = null
}

class Error(val reason: String?, val code: Int?, val type: ErrorType = ErrorType.HttpError)

enum class ErrorType{
    HttpError, ServerError
}