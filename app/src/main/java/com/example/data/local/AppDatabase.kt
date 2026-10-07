package com.example.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

// ─── Admin Credentials ────────────────────────────────────────────────────────
const val ADMIN_EMAIL    = "admin@homechef.com"
const val ADMIN_PASSWORD = "Admin@1234"

// ─── Entities ─────────────────────────────────────────────────────────────────

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val phone: String,
    val role: String,
    val photoUrl: String = "",
    val address: String = "",
    val isApprovedCook: Boolean = false,
    val isSuspended: Boolean = false
)

@Entity(tableName = "cart_items", primaryKeys = ["userId", "foodId"])
data class CartItemEntity(
    val userId: String,
    val foodId: String,
    val cookId: String,
    val cookName: String,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int,
    val specialRequest: String
)

@Entity(tableName = "favorites", primaryKeys = ["userId", "foodId"])
data class FavoriteEntity(
    val userId: String,
    val foodId: String
)

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("UPDATE users SET isSuspended = :suspended WHERE userId = :userId")
    suspend fun setSuspended(userId: String, suspended: Boolean)

    @Query("UPDATE users SET isApprovedCook = 1 WHERE userId = :userId")
    suspend fun approveCook(userId: String)

    @Query("UPDATE users SET name = :name, phone = :phone, address = :address WHERE userId = :userId")
    suspend fun updateProfile(userId: String, name: String, phone: String, address: String)

    @Query("UPDATE users SET photoUrl = :photoUrl WHERE userId = :userId")
    suspend fun updatePhoto(userId: String, photoUrl: String)
}

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

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [UserEntity::class, CartItemEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun cartDao(): CartDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "homechef_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance

                // Seed admin on first launch
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = instance.userDao()
                    if (dao.countUsers() == 0) {
                        dao.insertUser(
                            UserEntity(
                                userId       = "admin_001",
                                name         = "Admin",
                                email        = ADMIN_EMAIL,
                                passwordHash = hashPassword(ADMIN_PASSWORD),
                                phone        = "",
                                role         = "ADMIN",
                                isApprovedCook = false,
                                isSuspended    = false
                            )
                        )
                    }
                }
                instance
            }
        }

        fun hashPassword(raw: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
