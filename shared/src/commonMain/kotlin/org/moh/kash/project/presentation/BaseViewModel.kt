package org.moh.kash.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Common MVI BaseViewModel managing unidirectional data flow:
 * [Action] -> [onAction] -> [updateState] -> [State] -> UI
 * Side effects emitted via [Effect] -> [effects] channel.
 * Coroutines launched safely via [launchGuarded] with explicit error handling and CancellationException preservation.
 */
abstract class BaseViewModel<Action, State, Effect>(
    initialState: State
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    protected val currentState: State
        get() = _uiState.value

    abstract fun onAction(action: Action)

    protected fun updateState(reducer: (State) -> State) {
        _uiState.update(reducer)
    }

    protected fun emitEffect(effect: Effect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    /**
     * Executes an asynchronous coroutine block within [viewModelScope],
     * catching non-cancellation exceptions and delegating to [onError].
     * Never swallows [CancellationException].
     */
    protected fun launchGuarded(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                onError?.invoke(t)
            }
        }
    }
}
