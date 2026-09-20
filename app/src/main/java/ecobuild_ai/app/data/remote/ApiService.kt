package ecobuild_ai.app.data.remote

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.*

@Serializable
data class OrganizationResponse(
    val id: Int,
    val name: String,
    val location: String,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class OrganizationCreateRequest(
    val name: String,
    val location: String
)

@Serializable
data class PlanResponse(
    val id: Int,
    val organization_id: Int,
    val storage_reference: String,
    val format: String,
    val size: Int,
    val created_at: String
)

@Serializable
data class AnalysisResponse(
    val id: String,
    val plan_id: Int,
    val status: String, // "pending", "processing", "ready", "failed"
    val created_at: String,
    val material_list: MaterialListResponse? = null,
    val parent_analysis_id: String? = null,
    // Fields expected from backend (nullable until confirmed)
    val project_name: String? = null,
    val plan_type: String? = null,
    val sustainability_score: Int? = null,
    val co2_rating: String? = null,   // e.g. "Low", "Medium", "High"
    val sustain_notes: String? = null
) {
    // Convenience accessors — read from material_list where the API actually stores them
    val estimated_cost: Double? get() = material_list?.total_cost
    val co2_estimated: Double? get() = material_list?.co2_saved
}

@Serializable
data class MaterialListResponse(
    val id: String,
    val materials: List<MaterialItemResponse> = emptyList(),
    val notes: String? = null,
    val waste_percentage: Double? = null, // % de desperdício estimado
    val total_cost: Double? = null,       // custo total — API retorna aqui, não na raiz
    val co2_saved: Double? = null         // CO₂ poupado/estimado — API retorna aqui
)

@Serializable
data class MaterialItemResponse(
    val id: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val cost_per_unit: Double? = null,
    val eco_rating: String? = null        // e.g. "A", "B", "C"
)


@Serializable
data class AnalysisCreateRequest(
    val plan_id: Int
)

interface ApiService {
    @GET("/")
    suspend fun checkHealth(): Map<String, String>

    @GET("/organizations/")
    suspend fun getOrganizations(): List<OrganizationResponse>

    @POST("/organizations/")
    suspend fun createOrganization(@Body request: OrganizationCreateRequest): OrganizationResponse

    @Multipart
    @POST("/plans/{organization_id}")
    suspend fun uploadPlan(
        @Path("organization_id") orgId: Int,
        @Query("org_id") queryOrgId: Int, // Adicionado para bater com a query param do JSON
        @Part file: MultipartBody.Part
    ): PlanResponse

    @POST("/analyses/")
    suspend fun createAnalysis(@Body request: AnalysisCreateRequest): AnalysisResponse

    @GET("/analyses/{plan_id}")
    suspend fun getAnalyses(@Path("plan_id") planId: Int): List<AnalysisResponse>
}
