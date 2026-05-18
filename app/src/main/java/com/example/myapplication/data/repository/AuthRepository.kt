package com.example.myapplication.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.myapplication.data.model.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser get() = auth.currentUser

    suspend fun signIn(email: String, orgPassword: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, orgPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(name: String, email: String, orgPassword: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, orgPassword).await()
            val userId = result.user?.uid ?: throw Exception("User creation failed")
            
            val user = User(uid = userId, name = name, email = email)
            firestore.collection("users").document(userId).set(user).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        return try {
            val credentialManager = CredentialManager.create(context)
            
            // This WEB_CLIENT_ID should be retrieved from google-services.json client_type 3
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("754346873724-1cavdan2kgvisd6p22cdeidu8mmav0jh.apps.googleusercontent.com")
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is GoogleIdTokenCredential) {
                val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                    if (!userDoc.exists()) {
                        val newUser = User(
                            uid = firebaseUser.uid,
                            name = firebaseUser.displayName ?: "Eco User",
                            email = firebaseUser.email ?: ""
                        )
                        firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid credential type"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
