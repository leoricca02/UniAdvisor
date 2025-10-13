// file: repository/ApiClient.kt
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // L'IP 10.0.2.2 si riferisce al localhost del computer host dall'emulatore Android
    private const val BASE_URL = "http://10.0.2.2:5000/"

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}