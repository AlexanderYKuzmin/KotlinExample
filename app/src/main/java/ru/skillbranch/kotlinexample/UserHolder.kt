package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import kotlin.math.log

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        if (map.containsKey(email.lowercase())) throw IllegalArgumentException("A user with this email already exists")
        return User.makeUser(fullName, email = email, password = password)
            .also {
                map[it.login] = it
            }
    }

    fun registerUserByPhone(
        fullName: String,
        rawPhone: String,
    ): User {
        val phone = rawPhone.replace("[^+\\d]".toRegex(), "")
        if (phone.length != 12) throw IllegalArgumentException("Invalid phone number")
        if (map.containsKey(phone)) throw IllegalArgumentException("A user with this phone already exists")
        return User.makeUser(fullName, phone = phone)
            .also {
                map[it.login] = it
            }
    }

    fun loginUser(login: String, password: String): String? {

        val validLogin = getFormattedLogin(login)
        return map[validLogin.trim()]?.let{
            if (it.checkPassword(password)) {
                it.userInfo
            } else {
                null
            }
        }
    }

    fun requestAccessCode(login: String) {
        map[getFormattedLogin(login)]?.let {
            with(it) {
                changePassword(accessCode!!, generateAccessCode())
            }
        }
    }

    fun importUsers(list: List<String>): List<User> {
        val listOfUsers: MutableList<User> = ArrayList()
        list.forEach {
            val userStrArray = it.split(";")
            listOfUsers += User.makeUser(
                userStrArray[0],
                userStrArray[1],
                userStrArray[2],
                userStrArray[3]
            )
        }
        return listOfUsers
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

    private fun isLoginPhoneNumber(login: String): Boolean {
        return !login.contains("@")
    }

    private fun getFormattedLogin(login: String): String {
        return when(isLoginPhoneNumber(login)) {
            true -> login.replace("[^+\\d]".toRegex(), "")
            else -> login.lowercase().trim()
        }
    }
}