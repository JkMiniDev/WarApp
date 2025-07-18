package com.jkminidev.clashberry.network

import com.jkminidev.clashberry.data.ClanBasicInfo
import com.jkminidev.clashberry.data.WarResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    
    @GET("api/war/{clanTag}")
    suspend fun getWarData(@Path("clanTag") clanTag: String): Response<WarResponse>
    
    @GET("api/clan/{clanTag}")
    suspend fun getClanInfo(@Path("clanTag") clanTag: String): Response<ClanBasicInfo>
}