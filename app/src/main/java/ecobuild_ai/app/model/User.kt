package ecobuild_ai.app.model

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val createAt: Long = System.currentTimeMillis()
)
