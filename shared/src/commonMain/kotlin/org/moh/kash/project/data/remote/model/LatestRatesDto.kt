package org.moh.kash.project.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * FreeCurrencyAPI response DTO for /v1/latest.
 * Strictly internal to the data layer — never leaked to domain or presentation.
 */
@Serializable
internal data class LatestRatesDto(
    @SerialName("data") val data: Map<String, Double> = emptyMap()
)
