package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

// ─── Hardcoded Admin Credentials ───────────────────────────────────────────
const val ADMIN_EMAIL    = "admin@homechef.com"
const val ADMIN_PASSWORD = "Admin@1234"

private fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@Database(
    entities = [
        CartItemEntity::class,
        FavoriteEntity::class,
        UserProfileEntity::class,
        UserEntity::class
    ],
    version = 4,           // bumped from 3 → 4 for per-user cart items
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "homechef_connect_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance

                // Seed admin account on very first launch (runs async, safe)
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = instance.userDao()
                    if (dao.countUsers() == 0) {
                        dao.insertUser(
                            UserEntity(
                                userId       = "admin_001",
                                name         = "Admin",
                                email        = ADMIN_EMAIL,
                                passwordHash = sha256(ADMIN_PASSWORD),
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

        /** Utility exposed for login/register — same SHA-256 */
        fun hashPassword(raw: String): String = sha256(raw)
    }
}
