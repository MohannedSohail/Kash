package org.moh.kash.project.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * FreeCurrencyAPI response DTO for /v1/status.
 * Strictly internal to the data layer.
 */
@Serializable
internal data class StatusDto(
    @SerialName("account_id") val accountId: Long? = null,
    @SerialName("quotas") val quotas: QuotasDto? = null,
    @SerialName("plan") val plan: PlanDto? = null
)

@Serializable
internal data class QuotasDto(
    @SerialName("month") val month: MonthQuotaDto? = null
)

@Serializable
internal data class MonthQuotaDto(
    @SerialName("total") val total: Int = 1000,
    @SerialName("used") val used: Int = 0,
    @SerialName("remaining") val remaining: Int = 1000
)

@Serializable
internal data class PlanDto(
    @SerialName("name") val name: String? = "free",
    @SerialName("status") val status: String? = "active"
)
