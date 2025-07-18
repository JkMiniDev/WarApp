package com.jkminidev.clashberry.utils

import android.content.Context
import com.google.gson.Gson
import com.jkminidev.clashberry.R
import com.jkminidev.clashberry.data.ErrorResponse
import retrofit2.Response

object ErrorHandler {
    
    fun parseError(response: Response<*>): ErrorResponse {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                Gson().fromJson(errorBody, ErrorResponse::class.java)
            } else {
                ErrorResponse("api_error", "API Error: ${response.code()}", null)
            }
        } catch (e: Exception) {
            ErrorResponse("parse_error", "Failed to parse error response", null)
        }
    }
    
    fun getErrorDisplayText(context: Context, errorResponse: ErrorResponse): Pair<String, String> {
        return when (errorResponse.error) {
            "private_war_log" -> {
                Pair(
                    context.getString(R.string.private_war_log),
                    context.getString(R.string.private_war_log_message)
                )
            }
            "not_in_war" -> {
                Pair(
                    context.getString(R.string.not_in_war),
                    context.getString(R.string.not_in_war_message)
                )
            }
            "clan_not_found" -> {
                Pair(
                    context.getString(R.string.error_title),
                    context.getString(R.string.clan_not_found)
                )
            }
            "network_error" -> {
                Pair(
                    context.getString(R.string.error_title),
                    context.getString(R.string.network_error)
                )
            }
            else -> {
                Pair(
                    context.getString(R.string.error_title),
                    errorResponse.message
                )
            }
        }
    }
}