package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun getAllCartItems(userId: String): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE userId = :userId AND foodId = :foodId")
    suspend fun deleteByFoodId(userId: String, foodId: String)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT foodId FROM favorites WHERE userId = :userId")
    fun getFavoriteFoodIds(userId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND foodId = :foodId")
    suspend fun removeFavorite(userId: String, foodId: String)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(user: UserProfileEntity)
}

@Dao
interface UserDao {
    /** Insert a new user — fails silently if email already exists (IGNORE) */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity): Long

    /** Find user by email (for login lookup) */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    /** Check if ANY user exists yet (used to seed admin on first launch) */
    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    /** Get all registered users */
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    /** Update suspended flag */
    @Query("UPDATE users SET isSuspended = :suspended WHERE userId = :userId")
    suspend fun setSuspended(userId: String, suspended: Boolean)

    /** Approve cook */
    @Query("UPDATE users SET isApprovedCook = 1 WHERE userId = :userId")
    suspend fun approveCook(userId: String)

    /** Update profile fields */
    @Query("UPDATE users SET name = :name, phone = :phone, address = :address WHERE userId = :userId")
    suspend fun updateProfile(userId: String, name: String, phone: String, address: String)

    /** Update photo */
    @Query("UPDATE users SET photoUrl = :photoUrl WHERE userId = :userId")
    suspend fun updatePhoto(userId: String, photoUrl: String)

    /** Get single user by ID */
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun findById(userId: String): UserEntity?
}
