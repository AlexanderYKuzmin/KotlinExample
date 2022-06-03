package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String
    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().uppercaseChar() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]", "")
        }
    private var _login: String? = null
    var login: String
        set(value) {
            _login = value
        }
        get() = _login!!.lowercase(Locale.getDefault())

    private var salt: String? = null

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null


    //for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String
    ): this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary email constructor")
        passwordHash = encrypt(password)
    }

    //for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")){
        println("Secondary phone constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }

    //for csv
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String,
        phone: String?
    ) : this(firstName, lastName, email, phone, meta = mapOf("src" to "csv")) {
        salt = password.split(":")[0]
        passwordHash = password.split(":")[1]
    }

    init {
        println("First init block, primary constructor was called")

        check(firstName.isNotBlank()) {"FirstName Must not be blank"}
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) {"Email or Phone must not be null or blank"}

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) {
            passwordHash = encrypt(newPass)
            if (!accessCode.isNullOrEmpty()) accessCode = newPass
        } else {
            throw IllegalAccessException("The entered password does not match the current password")
        }
    }

    /*fun changeAccessCode() {
        accessCode?.let {
            val code = generateAccessCode()
            changePassword(it, code)
            accessCode = code
        }
    }*/

    private fun encrypt(password: String): String {
        if (salt.isNullOrEmpty()) {
            salt = ByteArray(16).also {
                SecureRandom().nextBytes(it)
            }.toString()
        }
        return salt.plus(password).md5()
    }

    fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6){
                (possible.indices).random().also {
                        index -> append(possible[index])
                }
            }
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String, code: String) {
        println(".......sending access code: $code on $phone")
    }

    private fun String.md5(): String {
        val md: MessageDigest = MessageDigest.getInstance("MD5")
        val digest: ByteArray = md.digest(toByteArray())
        val hexString: String = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName: String, lastName: String?) = fullName.fullNameToPair()

            return when {
                password?.contains(':') == true -> User(firstName, lastName, email, password, phone)
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastName, email, password)
                else -> throw IllegalArgumentException()
            }
        }

        private fun String.fullNameToPair() : Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when(size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException("FullName must contain only firstname" +
                                " and lastname, current split result ${this@fullNameToPair}")
                    }
                }
        }
    }
}