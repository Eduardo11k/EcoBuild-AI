package ecobuild_ai.app.model

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val organizationId: Int? = null,
    val createAt: Long = System.currentTimeMillis()
)
