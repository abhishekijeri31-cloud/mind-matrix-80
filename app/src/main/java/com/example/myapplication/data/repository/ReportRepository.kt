package com.example.myapplication.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.myapplication.data.model.Report
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class ReportRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun uploadReport(
        context: Context,
        userId: String,
        imageUri: Uri,
        wasteType: String,
        lat: Double,
        lng: Double,
        description: String
    ): Result<Unit> {
        return try {
            // 1. Compress Image
            val compressedImageData = compressImage(context, imageUri) ?: throw Exception("Image compression failed")

            // 2. Upload to Storage
            val fileName = UUID.randomUUID().toString()
            val imageRef = storage.reference.child("reports/$fileName.jpg")
            imageRef.putBytes(compressedImageData).await()
            val imageUrl = imageRef.downloadUrl.await().toString()

            // 3. Save Report to Firestore
            val reportId = firestore.collection("reports").document().id
            val report = Report(
                reportId = reportId,
                userId = userId,
                imageUrl = imageUrl,
                wasteType = wasteType,
                latitude = lat,
                longitude = lng,
                description = description
            )
            firestore.collection("reports").document(reportId).set(report).await()

            // 4. Update User Stats & Eco Points
            val userRef = firestore.collection("users").document(userId)
            firestore.runTransaction { transaction ->
                transaction.update(userRef, "ecoPoints", FieldValue.increment(50))
                transaction.update(userRef, "reportsSubmitted", FieldValue.increment(1))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun compressImage(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream) // 70% quality
            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    fun getReports(): Flow<List<Report>> = callbackFlow {
        val subscription = firestore.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reports = snapshot.toObjects(Report::class.java)
                    trySend(reports)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateReportStatus(reportId: String, userId: String, status: String): Result<Unit> {
        return try {
            firestore.collection("reports").document(reportId)
                .update("status", status).await()
            
            if (status == "Cleaned") {
                val userRef = firestore.collection("users").document(userId)
                firestore.runTransaction { transaction ->
                    transaction.update(userRef, "ecoPoints", FieldValue.increment(100))
                    transaction.update(userRef, "verifiedCleanups", FieldValue.increment(1))
                }.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
