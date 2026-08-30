package ecobuild_ai.app.repository

import ecobuild_ai.app.model.User
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    suspend fun saveUser(user: User) {
        usersCollection.document(user.uid).set(user).await()
    }

    suspend fun saveUserIfNotExists(user: User) {
        val doc = usersCollection.document(user.uid).get().await()
        if (!doc.exists()) {
           saveUser(user)
        } else {
            // Se o usuário já existe, podemos querer atualizar apenas alguns campos,
            // como a foto de perfil caso ela tenha mudado no Google (é importante cara).
            val updates = mutableMapOf<String, Any>()
            if (user.profileImageUrl.isNotEmpty()) {
                updates["profileImageUrl"] = user.profileImageUrl
            }
            if (updates.isNotEmpty()) {
                usersCollection.document(user.uid).update(updates).await()
            }
        }
    }

    suspend fun getUser(uid: String): User? {
        return usersCollection.document(uid).get().await().toObject(User::class.java)
    }

    suspend fun updateProfile(uid: String, fullName: String, username: String, email: String) {
        usersCollection.document(uid).update(
            mapOf(
                "fullName" to fullName,
                "email" to email,
                "username" to username
            )
        ).await()
    }
}
