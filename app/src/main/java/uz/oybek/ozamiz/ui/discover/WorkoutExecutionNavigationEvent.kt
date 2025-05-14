package uz.oybek.ozamiz.ui.discover

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.data.workouts.WorkoutExecutionState
import uz.oybek.ozamiz.data.workouts.WorkoutState
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.getinfo.Event
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

sealed class AudioCue {
    data class Resource(val resId: Int) : AudioCue()
    data class Url(val url: String) : AudioCue()
}

sealed class WorkoutExecutionNavigationEvent {
    object NavigateBack : WorkoutExecutionNavigationEvent()
    object WorkoutFinished : WorkoutExecutionNavigationEvent()
    data class ShowError(val message: String) : WorkoutExecutionNavigationEvent()
}

class WorkoutExecutionViewModel(application: Application) : AndroidViewModel(application) {

    private val PREPARE_TIME_SECONDS = 8
    private val DEFAULT_REST_TIME_SECONDS = 30
    private val COUNTDOWN_SECONDS = 3

    private val _state = MutableStateFlow(WorkoutExecutionState())
    val state: StateFlow<WorkoutExecutionState> = _state.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<WorkoutExecutionNavigationEvent>>()
    val navigationEvent: LiveData<Event<WorkoutExecutionNavigationEvent>> = _navigationEvent

    private val _audioCueEvent = MutableLiveData<Event<AudioCue?>>()
    val audioCueEvent: LiveData<Event<AudioCue?>> = _audioCueEvent

    private lateinit var exercises: List<Exercise>
    private var currentExerciseIndex = 0
    private var timerJob: Job? = null
    private var workoutTimerJob: Job? = null
    private var audioSequenceJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    private var totalElapsedSeconds = 0
    private var currentRestTime = DEFAULT_REST_TIME_SECONDS

    fun initialize(workoutId: String, exerciseList: List<Exercise>) {
        if (exerciseList.isEmpty()) {
            _state.update { it.copy(state = WorkoutState.FINISHED) }
            _navigationEvent.value =
                Event(WorkoutExecutionNavigationEvent.ShowError("Mashqlar ro'yxati bo'sh"))
            return
        }
        this.exercises = exerciseList
        this.currentExerciseIndex = 0
        this.totalElapsedSeconds = 0
        this.currentRestTime = DEFAULT_REST_TIME_SECONDS

        _state.update {
            WorkoutExecutionState(
                workoutId = workoutId,
                totalExercises = exercises.size,
                completedExercises = 0,
                elapsedWorkoutTimeSeconds = 0,
                estimatedCaloriesBurned = 0,
                state = WorkoutState.PREPARING
            )
        }
        startPreparationPhase()
    }

    override fun onCleared() {
        super.onCleared()
        cancelAllJobs()
        stopAndReleasePlayer()
    }

    private fun startPreparationPhase() {
        cancelTimerAndAudioJobs()
        val firstExercise = exercises.getOrNull(currentExerciseIndex)
        if (firstExercise == null) {
            finishWorkout()
            return
        }

        _state.update {
            it.copy(
                state = WorkoutState.PREPARING,
                currentExercise = firstExercise,
                nextExercise = exercises.getOrNull(currentExerciseIndex + 1),
                timeRemainingSeconds = PREPARE_TIME_SECONDS,
                timerProgress = 100
            )
        }
        viewModelScope.launch {
            playAudioCue(AudioCue.Resource(R.raw.tayyorlaning))
        }
        startTimer(PREPARE_TIME_SECONDS, WorkoutState.PREPARING, false) {
            startWorkingPhaseSequence()
        }

        if (workoutTimerJob == null || workoutTimerJob?.isActive == false) {
            startWorkoutTimer()
        }
    }

    private fun startWorkingPhaseSequence() {
        cancelTimerAndAudioJobs()
        val currentExercise = exercises.getOrNull(currentExerciseIndex)
        if (currentExercise == null) {
            finishWorkout()
            return
        }

        _state.update {
            it.copy(
                state = WorkoutState.WORKING,
                currentExercise = currentExercise,
                nextExercise = exercises.getOrNull(currentExerciseIndex + 1),
                timeRemainingSeconds = currentExercise.durationSeconds ?: 0,
                timerProgress = 100,
                completedExercises = currentExerciseIndex
            )
        }

        audioSequenceJob = viewModelScope.launch {
            currentExercise.exerciseNameAudioUrl?.let { url ->
                playAudioCue(AudioCue.Url(url), true)
            }

            playAudioCue(AudioCue.Resource(R.raw.three))
            delay(1000)
            playAudioCue(AudioCue.Resource(R.raw.two))
            delay(1000)
            playAudioCue(AudioCue.Resource(R.raw.one))
            delay(1000)
            playAudioCue(AudioCue.Resource(R.raw.boshlang))

            if (isActive && _state.value.state == WorkoutState.WORKING) {
                startActualWorkingPhase(currentExercise)
            }
        }
    }

    private fun startActualWorkingPhase(currentExercise: Exercise) {
        cancelTimerAndAudioJobs()

        val duration = currentExercise.durationSeconds ?: 0

        currentExercise.exerciseAboutAudioUrl?.let { url ->
            viewModelScope.launch {
                playAudioCue(AudioCue.Url(url))
            }
        }

        if (duration > 0) {
            _state.update { it.copy(timeRemainingSeconds = duration, timerProgress = 100) }
            startTimer(duration, WorkoutState.WORKING, true) {
                startRestPhase()
            }
        } else {
            _state.update {
                it.copy(
                    timeRemainingSeconds = DEFAULT_REST_TIME_SECONDS, timerProgress = 0
                )
            }
            viewModelScope.launch {
                if (isActive && _state.value.state == WorkoutState.WORKING) {
                    startRestPhase()
                }
            }
        }
    }

    private fun startRestPhase() {
        cancelTimerAndAudioJobs()
        currentRestTime = DEFAULT_REST_TIME_SECONDS

        val isLastExercise = currentExerciseIndex >= exercises.size - 1
        if (isLastExercise) {
            finishWorkout()
            return
        }

        val nextExercise = exercises.getOrNull(currentExerciseIndex + 1)

        _state.update {
            it.copy(
                state = WorkoutState.RESTING,
                nextExercise = nextExercise,
                timeRemainingSeconds = currentRestTime,
                timerProgress = 100
            )
        }
        viewModelScope.launch {
            playAudioCue(AudioCue.Resource(R.raw.dam_oling))
        }
        startTimer(currentRestTime, WorkoutState.RESTING, false) {
            currentExerciseIndex++
            startWorkingPhaseSequence()
        }
    }

    private fun startTimer(
        durationSeconds: Int,
        forState: WorkoutState,
        playCountdownAtEnd: Boolean,
        onFinish: () -> Unit
    ) {
        timerJob?.cancel()
        if (durationSeconds <= 0) {
            onFinish()
            return
        }

        timerJob = viewModelScope.launch {
            try {
                for (time in durationSeconds downTo 0) {
                    while (_state.value.state == WorkoutState.PAUSED && isActive) {
                        delay(300)
                    }

                    if (_state.value.state == forState) {
                        val progress =
                            ((time.toFloat() / durationSeconds.toFloat()) * 100).roundToInt()
                                .coerceIn(0, 100)
                        _state.update {
                            it.copy(timeRemainingSeconds = time, timerProgress = progress)
                        }
                    }

                    if (playCountdownAtEnd && time in 1..COUNTDOWN_SECONDS && _state.value.state == forState) {
                        when (time) {
                            3 -> playAudioCue(AudioCue.Resource(R.raw.three))
                            2 -> playAudioCue(AudioCue.Resource(R.raw.two))
                            1 -> playAudioCue(AudioCue.Resource(R.raw.one))
                        }
                    }

                    if (time > 0) {
                        delay(1000)
                    }
                }

                if (isActive) {
                    if (_state.value.state == forState || forState == WorkoutState.WORKING && _state.value.state == WorkoutState.PAUSED) {
                        onFinish()
                    }
                }
            } catch (e: CancellationException) {
                // Timer cancelled
            } catch (e: Exception) {
                handleException(e)
                _navigationEvent.postValue(Event(WorkoutExecutionNavigationEvent.ShowError("Taymerda xatolik")))
            }
        }
    }

    private fun startWorkoutTimer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (isActive && _state.value.state != WorkoutState.FINISHED) {
                if (_state.value.state != WorkoutState.PAUSED) {
                    totalElapsedSeconds++
                    val currentExerciseForCal =
                        if (_state.value.state == WorkoutState.WORKING) _state.value.currentExercise else null
                    _state.update {
                        it.copy(
                            elapsedWorkoutTimeSeconds = totalElapsedSeconds,
                            estimatedCaloriesBurned = calculateEstimatedCalories(
                                totalElapsedSeconds, currentExerciseForCal
                            )
                        )
                    }
                }
                delay(1000)
            }
        }
    }

    private fun finishWorkout() {
        cancelAllJobs()
        _state.update {
            it.copy(
                state = WorkoutState.FINISHED,
                completedExercises = exercises.size,
                timeRemainingSeconds = 0,
                timerProgress = 0
            )
        }
        viewModelScope.launch {
            playAudioCue(AudioCue.Resource(R.raw.tabriklaymiz))
        }
        _navigationEvent.value = Event(WorkoutExecutionNavigationEvent.WorkoutFinished)
    }

    fun pauseResumeWorkout() {
        val currentState = _state.value.state
        if (currentState == WorkoutState.PAUSED) {
            resumeWorkout()
        } else {
            pauseWorkout()
        }
    }

    private fun pauseWorkout() {
        if (_state.value.state.isActiveState()) {
            workoutTimerJob?.cancel()
            pausedStateBackup = _state.value.state
            cancelTimerAndAudioJobs()
            stopAndReleasePlayer()
            _state.update { it.copy(state = WorkoutState.PAUSED) }
        }
    }

    private fun resumeWorkout() {
        if (_state.value.state == WorkoutState.PAUSED) {
            pausedStateBackup?.let { stateBeforePause ->
                _state.update { it.copy(state = stateBeforePause) }
                when (stateBeforePause) {
                    WorkoutState.PREPARING -> startTimer(_state.value.timeRemainingSeconds, WorkoutState.PREPARING, false) { startWorkingPhaseSequence() }
                    WorkoutState.WORKING -> startTimer(_state.value.timeRemainingSeconds, WorkoutState.WORKING, true) { startRestPhase() }
                    WorkoutState.RESTING -> {
                        //Restni to'g'ri tiklash uchun, boshlang'ich dam olish vaqtidan tiklash kerak
                        startTimer(currentRestTime, WorkoutState.RESTING, false) { currentExerciseIndex++; startWorkingPhaseSequence() }
                    }
                    else -> {}
                }
            } ?: run {
                if (currentExerciseIndex < exercises.size) startPreparationPhase() else finishWorkout()
            }
            pausedStateBackup = null
            startWorkoutTimer()
        }
    }

    private var pausedStateBackup: WorkoutState? = null

    fun skip() {
        stopAndReleasePlayer()
        cancelTimerAndAudioJobs()

        val currentState = _state.value.state
        when (currentState) {
            WorkoutState.PREPARING -> {
                startWorkingPhaseSequence()
            }

            WorkoutState.WORKING -> {
                startRestPhase()
            }

            WorkoutState.RESTING -> {
                currentExerciseIndex++
                if (currentExerciseIndex < exercises.size) {
                    startWorkingPhaseSequence()
                } else {
                    finishWorkout()
                }
            }

            WorkoutState.PAUSED -> {
                val stateBeforePause = pausedStateBackup
                pausedStateBackup = null
                _state.update { it.copy(state = stateBeforePause ?: WorkoutState.PREPARING) }
                when (stateBeforePause) {
                    WorkoutState.PREPARING -> startWorkingPhaseSequence()
                    WorkoutState.WORKING -> startRestPhase()
                    WorkoutState.RESTING -> {
                        currentExerciseIndex++
                        if (currentExerciseIndex < exercises.size) startPreparationPhase() else finishWorkout()
                    }

                    else -> {
                        if (currentExerciseIndex < exercises.size) startPreparationPhase() else finishWorkout()
                    }
                }

            }

            WorkoutState.FINISHED -> {} // Already finished
        }
    }

    fun previous() {
        stopAndReleasePlayer()
        cancelAllJobs()

        if (currentExerciseIndex > 0) {
            currentExerciseIndex--
            _state.update { it.copy(state = WorkoutState.PREPARING) }
            startPreparationPhase()
        } else {
            _navigationEvent.value =
                Event(WorkoutExecutionNavigationEvent.ShowError("Bu birinchi mashq"))
            startPreparationPhase()
        }
    }

    fun addRestTime() {
        if (_state.value.state == WorkoutState.RESTING) {
            val currentRemaining = _state.value.timeRemainingSeconds
            val newRestTime = currentRemaining + 20
            timerJob?.cancel()

            _state.update {
                it.copy(
                    timeRemainingSeconds = newRestTime
                )
            }
            startTimer(newRestTime, WorkoutState.RESTING, false) {
                currentExerciseIndex++
                if (currentExerciseIndex < exercises.size) {
                    startPreparationPhase()
                } else {
                    finishWorkout()
                }
            }
        }
    }

    fun handleExitConfirmation() {
        cancelAllJobs()
        stopAndReleasePlayer()
        _state.update { it.copy(state = WorkoutState.FINISHED) }
        _navigationEvent.value = Event(WorkoutExecutionNavigationEvent.NavigateBack)
    }

    private suspend fun playAudioCue(
        cue: AudioCue?, blockSuspend: Boolean = false
    ) = suspendCoroutine {
        if (cue == null) {
            it.resume(Any())
            return@suspendCoroutine
        }
        var done: (() -> Unit)? = null
        if (blockSuspend) {
            done = {
                it.resume(null)
            }
        } else {
            it.resume(null)
        }
        playAudio(cue, done)
    }

    fun stopAndReleasePlayer() {
        if (mediaPlayer == null) return
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: IllegalStateException) {
            try {
                mediaPlayer?.release()
            } catch (e2: Exception) { /* Ignore inner */
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaPlayer = null
        }
    }


    private fun playAudio(cue: AudioCue, onDone: (() -> Unit)? = {}, after: Long = 0) {
        viewModelScope.launch {
            stopAndReleasePlayer()
            if (after > 0) {
                delay(after)
            }
            var done = false
            try {
                val context = getApplication<Application>().applicationContext
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA).build()
                    )
                    setOnPreparedListener {
                        done = true
                    }
                    setOnErrorListener { mp, what, extra ->
                        onDone?.invoke()
                        stopAndReleasePlayer()
                        done = true
                        true
                    }
                    setOnCompletionListener {
                        done = true
                        onDone?.invoke()
                    }

                    when (cue) {
                        is AudioCue.Resource -> {
                            val afd = context.resources.openRawResourceFd(cue.resId) ?: return@apply
                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                            prepare()
                            start()
                        }

                        is AudioCue.Url -> {
                            setDataSource(cue.url)
                            setOnPreparedListener { mp ->
                                mp.start()
                            }
                            prepareAsync()
                        }
                    }
                }

            } catch (e: Exception) {
                stopAndReleasePlayer()
                done = true
                onDone?.invoke()
            }
            delay(3000)
            if (done.not()) {
                onDone?.invoke()
            }
        }
    }

    private fun cancelTimerAndAudioJobs() {
        timerJob?.cancel()
        audioSequenceJob?.cancel()
    }

    private fun cancelAllJobs() {
        timerJob?.cancel()
        audioSequenceJob?.cancel()
        workoutTimerJob?.cancel()
    }

    private fun calculateEstimatedCalories(elapsedSeconds: Int, exercise: Exercise?): Int {
        val activeSeconds = totalElapsedSeconds
        val rate =
            if (_state.value.state == WorkoutState.WORKING && exercise != null) 0.12 else 0.02
        return (activeSeconds * rate).roundToInt()
    }

    private fun WorkoutState.isActiveState(): Boolean {
        return this == WorkoutState.PREPARING || this == WorkoutState.WORKING || this == WorkoutState.RESTING
    }
}