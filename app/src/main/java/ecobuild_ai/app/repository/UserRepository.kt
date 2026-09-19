package ecobuild_ai.app.repository

import ecobuild_ai.app.model.User
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    suspend fun saveUser(user: User) {
        try {
            usersCollection.document(user.uid).set(user).await()
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error saving user to Firestore: ${e.message}", e)
        }
    }

    suspend fun saveUserIfNotExists(user: User) {
        try {
            val doc = usersCollection.document(user.uid).get().await()
            if (!doc.exists()) {
                saveUser(user)
            } else {
                val updates = mutableMapOf<String, Any>()
                if (user.profileImageUrl.isNotEmpty()) {
                    updates["profileImageUrl"] = user.profileImageUrl
                }
                if (user.fullName.isNotEmpty()) {
                    val currentFullName = doc.getString("fullName")
                    if (currentFullName.isNullOrEmpty()) {
                        updates["fullName"] = user.fullName
                    }
                }

                if (updates.isNotEmpty()) {
                    usersCollection.document(user.uid).update(updates).await()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error in saveUserIfNotExists: ${e.message}", e)
        }
    }

    suspend fun getUser(uid: String): User? {
        return try {
            usersCollection.document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error fetching user: ${e.message}", e)
            null
        }
    }

    suspend fun updateOrganization(uid: String, orgId: Int) {
        try {
            usersCollection.document(uid).update("organizationId", orgId).await()
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error updating organizationId: ${e.message}", e)
        }
    }

    suspend fun updateProfile(uid: String, fullName: String, email: String, profileImageUrl: String = "") {
        try {
            val updates = mutableMapOf<String, Any>(
                "fullName" to fullName,
                "email" to email
            )
            if (profileImageUrl.isNotEmpty()) {
                updates["profileImageUrl"] = profileImageUrl
            }
            usersCollection.document(uid).update(updates).await()
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error updating profile: ${e.message}", e)
            val newUser = User(
                uid = uid,
                fullName = fullName,
                email = email,
                profileImageUrl = profileImageUrl
            )
            saveUser(newUser)
        }
    }
}
