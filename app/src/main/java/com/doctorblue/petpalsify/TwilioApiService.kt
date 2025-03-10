import retrofit2.Call
import retrofit2.http.*

interface TwilioApiService {
    @FormUrlEncoded
    @POST("Services/{serviceSid}/Verifications")
    fun sendOTP(
        @Path("serviceSid") serviceSid: String,
        @Field("To") phoneNumber: String,
        @Field("Channel") channel: String = "sms" // Default SMS
    ): Call<TwilioResponse>

    @FormUrlEncoded
    @POST("Services/{serviceSid}/VerificationCheck")
    fun verifyOTP(
        @Path("serviceSid") serviceSid: String,
        @Field("To") phoneNumber: String,
        @Field("Code") code: String
    ): Call<TwilioResponse>
}
