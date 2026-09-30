package com.rivavafi.universal.di

import com.rivavafi.universal.domain.api.CryptoApiService
import com.rivavafi.universal.domain.api.StockApiService
import com.rivavafi.universal.domain.api.ChatApiService
import com.rivavafi.universal.domain.api.CoinGeckoApiService
import com.rivavafi.universal.domain.api.YahooCryptoApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("finnhub")
    fun provideFinnhubRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    @Provides
    @Singleton
    @Named("yahoo")
    fun provideYahooRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("coingecko")
    fun provideCoinGeckoRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("chat")
    fun provideChatRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            // Placeholder base URL for the chatbot API as requested
            .baseUrl("https://api.rivava.in/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideStockApiService(@Named("finnhub") retrofit: Retrofit): StockApiService {
        return retrofit.create(StockApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCryptoApiService(@Named("finnhub") retrofit: Retrofit): CryptoApiService {
        return retrofit.create(CryptoApiService::class.java)
    }


    @Provides
    @Singleton
    fun provideYahooCryptoApiService(@Named("yahoo") retrofit: Retrofit): YahooCryptoApiService {
        return retrofit.create(YahooCryptoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCoinGeckoApiService(@Named("coingecko") retrofit: Retrofit): CoinGeckoApiService {
        return retrofit.create(CoinGeckoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideChatApiService(@Named("chat") retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }

}
