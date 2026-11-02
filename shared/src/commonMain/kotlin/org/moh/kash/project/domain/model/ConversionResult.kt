package org.moh.kash.project.domain.model

/**
 * Domain entity representing the result of a currency conversion calculation.
 * Preserves unrounded precision in numeric fields alongside target-currency formatted displays.
 */
data class ConversionResult(
    val sourceAmount: Double,
    val baseCurrency: String,
    val targetCurrency: Currency,
    val rate: Double,
    val convertedAmount: Double,
    val formattedAmount: String,
    val formattedRate: String
)
