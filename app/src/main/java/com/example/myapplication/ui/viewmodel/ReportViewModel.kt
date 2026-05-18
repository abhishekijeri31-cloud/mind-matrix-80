package com.example.myapplication.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Report
import com.example.myapplication.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ReportViewModel(private val repository: ReportRepository = ReportRepository()) : ViewModel() {

    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports

    init {
        observeReports()
    }

    private fun observeReports() {
        viewModelScope.launch {
            // Added .catch to prevent the app from crashing if Firestore permissions are denied
            repository.getReports()
                .catch { e ->
                    Log.e("ReportViewModel", "Error observing reports: ${e.message}")
                    _reportState.value = ReportState.Error("Database access denied. Check your Firebase Rules.")
                }
                .collect { list ->
                    _reports.value = list
                }
        }
    }

    fun submitReport(
        context: Context,
        userId: String,
        imageUri: Uri,
        wasteType: String,
        lat: Double,
        lng: Double,
        description: String
    ) {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading
            try {
                val result = repository.uploadReport(context, userId, imageUri, wasteType, lat, lng, description)
                if (result.isSuccess) {
                    _reportState.value = ReportState.Success
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Upload Failed"
                    _reportState.value = ReportState.Error(errorMsg)
                    Log.e("ReportViewModel", "Upload error: $errorMsg")
                }
            } catch (e: Exception) {
                _reportState.value = ReportState.Error(e.message ?: "An unexpected error occurred")
                Log.e("ReportViewModel", "Critical failure during submit: ${e.message}")
            }
        }
    }

    fun markAsCleaned(reportId: String, volunteerId: String) {
        viewModelScope.launch {
            try {
                repository.updateReportStatus(reportId, volunteerId, "Cleaned")
            } catch (e: Exception) {
                Log.e("ReportViewModel", "Error marking as cleaned: ${e.message}")
            }
        }
    }

    fun resetState() {
        _reportState.value = ReportState.Idle
    }
}

sealed class ReportState {
    object Idle : ReportState()
    object Loading : ReportState()
    object Success : ReportState()
    data class Error(val message: String) : ReportState()
}