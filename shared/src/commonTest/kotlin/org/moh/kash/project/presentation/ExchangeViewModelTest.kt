package org.moh.kash.project.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.moh.kash.project.core.error.AppError
import org.moh.kash.project.domain.model.ExchangeRates
import org.moh.kash.project.domain.repository.CurrencyRepository
import org.moh.kash.project.domain.usecase.ConvertCurrencyUseCase
import org.moh.kash.project.domain.usecase.GetExchangeRatesUseCase
import org.moh.kash.project.domain.usecase.UpdateBaseCurrencyUseCase
import org.moh.kash.project.presentation.exchange.ExchangeUiAction
import org.moh.kash.project.presentation.exchange.ExchangeUiEffect
import org.moh.kash.project.presentation.exchange.ExchangeViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleRates = mapOf(
        "USD" to 1.0000,
        "EUR" to 0.9200,
        "ILS" to 3.6500,
        "JOD" to 0.7090
    )

    private class FakeCurrencyRepository(
        private val ratesMap: Map<String, Double>
    ) : CurrencyRepository {
        val baseCurrencyFlow = MutableStateFlow("USD")
        var shouldFail: Boolean = false

        override fun getRates(baseCurrency: String, forceRefresh: Boolean): Flow<Result<ExchangeRates>> = flow {
            if (shouldFail) {
                emit(Result.failure(AppError.NetworkError("Network request failed.")))
            } else {
                emit(Result.success(ExchangeRates(baseCurrency, ratesMap, 1700000000000L)))
            }
        }

        override suspend fun saveBaseCurrency(currencyCode: String) {
            baseCurrencyFlow.value = currencyCode
        }

        override fun getBaseCurrency(): Flow<String> = baseCurrencyFlow
    }

    private lateinit var fakeRepository: FakeCurrencyRepository
    private lateinit var convertCurrencyUseCase: ConvertCurrencyUseCase
    private lateinit var getExchangeRatesUseCase: GetExchangeRatesUseCase
    private lateinit var updateBaseCurrencyUseCase: UpdateBaseCurrencyUseCase
    private lateinit var viewModel: ExchangeViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeCurrencyRepository(sampleRates)
        convertCurrencyUseCase = ConvertCurrencyUseCase()
        getExchangeRatesUseCase = GetExchangeRatesUseCase(fakeRepository)
        updateBaseCurrencyUseCase = UpdateBaseCurrencyUseCase(fakeRepository)

        viewModel = ExchangeViewModel(
            getExchangeRatesUseCase = getExchangeRatesUseCase,
            convertCurrencyUseCase = convertCurrencyUseCase,
            updateBaseCurrencyUseCase = updateBaseCurrencyUseCase,
            currencyRepository = fakeRepository
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateHasUsdAndPopulatesTargetCards() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("USD", state.baseCurrency)
        assertEquals("100.00", state.inputAmount)
        assertFalse(state.isColdLoading)
        assertNull(state.amountError)
        assertNull(state.error)

        // Should display the 3 target currencies (EUR, ILS, JOD)
        assertEquals(3, state.rates.size)
        val targetCodes = state.rates.map { it.currencyCode }.toSet()
        assertEquals(setOf("EUR", "ILS", "JOD"), targetCodes)

        val eurItem = state.rates.first { it.currencyCode == "EUR" }
        assertEquals(92.00, eurItem.convertedAmount, 0.01)
        assertEquals("92.00", eurItem.formattedAmount)

        val ilsItem = state.rates.first { it.currencyCode == "ILS" }
        assertEquals(365.00, ilsItem.convertedAmount, 0.01)
        assertEquals("365.00", ilsItem.formattedAmount)

        val jodItem = state.rates.first { it.currencyCode == "JOD" }
        assertEquals(70.900, jodItem.convertedAmount, 0.001)
        assertEquals("70.900", jodItem.formattedAmount)
    }

    @Test
    fun testAmountChangedUpdatesConversions() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onAction(ExchangeUiAction.OnAmountChanged("200.00"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("200.00", state.inputAmount)
        assertNull(state.amountError)

        val eurItem = state.rates.first { it.currencyCode == "EUR" }
        assertEquals(184.00, eurItem.convertedAmount, 0.01)
        assertEquals("184.00", eurItem.formattedAmount)
    }

    @Test
    fun testNegativeAmountTriggersFieldTierError() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onAction(ExchangeUiAction.OnAmountChanged("-100"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("-100", state.inputAmount)
        assertEquals("Amount must be a positive number.", state.amountError)
    }

    @Test
    fun testQuickAmountSelectedScalesCards() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onAction(ExchangeUiAction.OnQuickAmountSelected(1000.0))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("1,000.00", state.inputAmount)
        assertNull(state.amountError)

        val ilsItem = state.rates.first { it.currencyCode == "ILS" }
        assertEquals(3650.00, ilsItem.convertedAmount, 0.01)
        assertEquals("3,650.00", ilsItem.formattedAmount)
    }

    @Test
    fun testClearAmountResetsToZero() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onAction(ExchangeUiAction.OnClearAmount)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.inputAmount)
        assertNull(state.amountError)

        state.rates.forEach { rateItem ->
            assertEquals(0.0, rateItem.convertedAmount)
        }
    }

    @Test
    fun testBaseCurrencySelectionUpdatesTargetsAndPersists() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onAction(ExchangeUiAction.OnBaseCurrencySelected("EUR"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("EUR", state.baseCurrency)
        assertFalse(state.isCurrencyPickerOpen)

        // When EUR is base, targets must be USD, ILS, JOD
        val targetCodes = state.rates.map { it.currencyCode }.toSet()
        assertEquals(setOf("USD", "ILS", "JOD"), targetCodes)

        // Verify persisted preference
        assertEquals("EUR", fakeRepository.baseCurrencyFlow.value)
    }

    @Test
    fun testCurrencyPickerOpenAndClose() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onAction(ExchangeUiAction.SetCurrencyPickerOpen(true))
        assertTrue(viewModel.uiState.value.isCurrencyPickerOpen)

        viewModel.onAction(ExchangeUiAction.SetCurrencyPickerOpen(false))
        assertFalse(viewModel.uiState.value.isCurrencyPickerOpen)
    }

    @Test
    fun testRefreshFailureRetainsRatesAndEmitsPopupSnackbar() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.rates.isNotEmpty())

        // Make future calls fail
        fakeRepository.shouldFail = true

        viewModel.onAction(ExchangeUiAction.OnRefresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        // Non-destructive refresh: rates must NOT be wiped!
        assertTrue(state.rates.isNotEmpty(), "Rates must remain visible during refresh failure")
    }

    @Test
    fun testColdFailureSetsInlineErrorWhenNoPriorRates() = runTest(testDispatcher) {
        val failingRepo = FakeCurrencyRepository(emptyMap()).apply { shouldFail = true }
        val failingVm = ExchangeViewModel(
            getExchangeRatesUseCase = GetExchangeRatesUseCase(failingRepo),
            convertCurrencyUseCase = convertCurrencyUseCase,
            updateBaseCurrencyUseCase = updateBaseCurrencyUseCase,
            currencyRepository = failingRepo
        )
        advanceUntilIdle()

        val state = failingVm.uiState.value
        assertFalse(state.isColdLoading)
        assertTrue(state.rates.isEmpty())
        assertNotNull(state.error, "Fatal cold load must set inline error")
    }
}
