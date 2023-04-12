package domain

data class User(
    val name: String,
    private val password: String,
) {
    var lastChannel: Channel? = null

    fun check(password: String): Boolean = this.password == password
}

