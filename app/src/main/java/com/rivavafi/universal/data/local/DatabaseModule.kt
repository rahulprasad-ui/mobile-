package com.rivavafi.universal.data.local

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import android.content.Context
import com.rivavafi.universal.data.preferences.UserPreferencesRepository
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRivavaDatabase(app: Application): RivavaDatabase {
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN billingCycle TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN lastPaymentDate INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN referenceId TEXT")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN subcategory TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_corrections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `merchantName` TEXT NOT NULL, `keyword` TEXT, `category` TEXT NOT NULL, `subcategory` TEXT)")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE categories ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_corrections ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN upiId TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountNumberLast4 TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN transactionId TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN smsSender TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'")
                val currentTime = System.currentTimeMillis()
                db.execSQL("ALTER TABLE transactions ADD COLUMN createdAt INTEGER NOT NULL DEFAULT $currentTime")
                db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $currentTime")
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN documentId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN description TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN notes TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT")
            }
        }

        return Room.databaseBuilder(
            app,
            RivavaDatabase::class.java,
            "trackfi_db"
        )
        .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(db: RivavaDatabase): TransactionDao {
        return db.transactionDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(db: RivavaDatabase): CategoryDao {
        return db.categoryDao()
    }

    @Provides
    @Singleton
    fun provideUserCorrectionDao(db: RivavaDatabase): UserCorrectionDao {
        return db.userCorrectionDao()
    }

    @Provides
    @Singleton
    fun provideCalculatorHistoryDao(db: RivavaDatabase): CalculatorHistoryDao {
        return db.calculatorHistoryDao()
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(app: Application): UserPreferencesRepository {
        return UserPreferencesRepository(app.applicationContext)
    }
}
