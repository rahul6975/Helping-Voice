package com.rahul.helpingvoice.Service
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesService {
    @GET("/search?media=podcast")
    fun searchPodcastByTerm(@Query("term") term: String): Call<PodcastResponse>    companion object {
        val instance: ItunesService by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://itunes.apple.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            retrofit.create<ItunesService>(ItunesService::class.java)
        }
    }
}