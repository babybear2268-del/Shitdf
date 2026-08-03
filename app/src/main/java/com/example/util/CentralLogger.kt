package com.example.util

import android.util.Log
import com.example.data.OrchestratorRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INFO, WARN, ERROR, EXEC, STATE_CHANGE
}

data class LogEntry(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val timestamp: Long = System.currentTimeMillis(),
    val timestampFormatted: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp)),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val stackTrace: String? = null,
    val jobId: String? = null
)

object CentralLogger {
    private const val TAG = "CentralLogger"
    private var repository: OrchestratorRepository? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _recentLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val recentLogs: StateFlow<List<LogEntry>> = _recentLogs.asStateFlow()

    fun initialize(repo: OrchestratorRepository) {
        this.repository = repo
        log(LogLevel.INFO, TAG, "CentralLogger initialized successfully")
    }

    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        jobId: String? = null
    ) {
        val stackTraceString = throwable?.let { extractStackTrace(it) }
        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            stackTrace = stackTraceString,
            jobId = jobId
        )

        // Output to Logcat
        when (level) {
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, "$message\n${stackTraceString ?: ""}", throwable)
            LogLevel.EXEC -> Log.d(tag, message, throwable)
            LogLevel.STATE_CHANGE -> Log.i("StateTransition", "[$tag] $message")
        }

        // Buffer in memory
        synchronized(this) {
            val current = _recentLogs.value.toMutableList()
            current.add(0, entry)
            if (current.size > 200) {
                _recentLogs.value = current.take(200)
            } else {
                _recentLogs.value = current
            }
        }

        // Store in Room via OrchestratorRepository
        repository?.let { repo ->
            scope.launch {
                val formattedMsg = if (stackTraceString != null) {
                    "[$tag] $message\nStack: ${stackTraceString.take(500)}"
                } else {
                    "[$tag] $message"
                }
                repo.insertLog(level.name, formattedMsg, jobId)
            }
        }
    }

    fun logStateTransition(tag: String, stateName: String, oldState: String, newState: String) {
        val msg = "$stateName changed: [$oldState] ➔ [$newState]"
        log(LogLevel.STATE_CHANGE, tag, msg)
    }

    fun logNetworkError(
        tag: String = "NetworkService",
        endpoint: String,
        errorMsg: String,
        httpCode: Int? = null,
        throwable: Throwable? = null
    ) {
        val codeInfo = if (httpCode != null) " (HTTP $httpCode)" else ""
        val fullMsg = "Network Error at $endpoint$codeInfo: $errorMsg"
        log(LogLevel.ERROR, tag, fullMsg, throwable)
    }

    private fun extractStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
}
