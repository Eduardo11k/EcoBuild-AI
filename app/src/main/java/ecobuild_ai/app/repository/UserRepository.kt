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
            android.util.Log.e("UserRepository", "Erro ao salvar usuario no Firestore: ${e.message}", e)
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
                val currentUsername = doc.getString("username")
                if (currentUsername.isNullOrEmpty() && user.username.isNotEmpty()) {
                    updates["username"] = user.username
                }

                if (updates.isNotEmpty()) {
                    usersCollection.document(user.uid).update(updates).await()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Erro em saveUserIfNotExists: ${e.message}", e)
        }
    }

    suspend fun getUser(uid: String): User? {
        return try {
            usersCollection.document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Erro ao buscar usuario: ${e.message}", e)
            null
        }
    }

    suspend fun updateProfile(uid: String, fullName: String, username: String, email: String, profileImageUrl: String = "") {
        try {
            val updates = mutableMapOf<String, Any>(
                "fullName" to fullName,
                "email" to email,
                "username" to username
            )
            if (profileImageUrl.isNotEmpty()) {
                updates["profileImageUrl"] = profileImageUrl
            }
            usersCollection.document(uid).update(updates).await()
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Erro ao atualizar perfil: ${e.message}", e)
            // Se o documento ainda não existia por algum motivo, cria-o agora com set
            val newUser = User(
                uid = uid,
                fullName = fullName,
                username = username,
                email = email,
                profileImageUrl = profileImageUrl
            )
            saveUser(newUser)
        }
    }
}
