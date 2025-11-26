package com.example.detectai

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import com.google.gson.annotations.SerializedName

// ============ TEXT DETECTION MODELS ============
data class AnalyzeRequest(val text: String)

data class AnalyzeResponse(
    val prediction: String,
    @SerializedName("ai_probability") val ai_probability: Double,
    @SerializedName("human_probability") val human_probability: Double,
    val confidence: String,
    @SerializedName("ai_indicators") val ai_indicators: List<Indicator>,
    @SerializedName("human_indicators") val human_indicators: List<Indicator>
)

data class Indicator(
    val sentence: String,
    val score: Double,
    val reason: String
)

// ============ IMAGE DETECTION MODELS ============
data class ImageAnalyzeRequest(
    val image: String  // Base64 encoded image
)

data class ImageAnalyzeResponse(
    val prediction: String,
    @SerializedName("ai_probability") val ai_probability: Double,
    @SerializedName("real_probability") val real_probability: Double,
    val confidence: String,
    val explanations: List<ImageExplanation>
)

data class ImageExplanation(
    val indicator: String,
    val description: String,
    val type: String  // "AI", "Real", or "Neutral"
)

// ============ HEALTH CHECK ============
data class HealthResponse(
    val status: String,
    val message: String? = null,
    @SerializedName("text_model") val text_model: String? = null,
    @SerializedName("image_model") val image_model: String? = null
)

// ============ API INTERFACE ============
interface DetectionAPI {
    @GET("health")  // ✅ No leading slash
    suspend fun checkHealth(): HealthResponse

    @POST("analyze")  // ✅ No leading slash
    suspend fun analyzeText(@Body request: AnalyzeRequest): AnalyzeResponse

    @POST("analyze-image")  // ✅ No leading slash
    suspend fun analyzeImage(@Body request: ImageAnalyzeRequest): ImageAnalyzeResponse
}

// ============ API CLIENT ============
object DetectionAPIClient {
    private const val BASE_URL = "https://kushalkv-detectai-api.hf.space/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: DetectionAPI = retrofit.create(DetectionAPI::class.java)
}
