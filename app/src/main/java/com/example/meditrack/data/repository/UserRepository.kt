package com.example.meditrack.data.repository

import com.example.meditrack.data.local.dao.UserDao
import com.example.meditrack.data.local.entity.UserProfile
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    val userProfile: Flow<UserProfile?> = userDao.getUserProfile()

    suspend fun getUserProfileOnce(): UserProfile? {
        return userDao.getUserProfileOnce()
    }

    suspend fun saveUserProfile(fullName: String): Long {
        val existing = userDao.getUserProfileOnce()
        val user = if (existing != null) {
            existing.copy(fullName = fullName)
        } else {
            UserProfile(fullName = fullName)
        }
        return userDao.insertUser(user)
    }

    suspend fun hasUserProfile(): Boolean {
        return userDao.getUserProfileOnce() != null
    }
}
