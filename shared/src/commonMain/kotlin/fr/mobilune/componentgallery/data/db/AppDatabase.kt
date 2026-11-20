package fr.mobilune.componentgallery.data.db

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

const val DATABASE_FILE_NAME = "componentgallery.db"

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val merchant: String,
    val isoDate: String,
    val amountCents: Long,
    // Conserve l'ordre renvoyé par l'API.
    val position: Int,
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY position")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(transactions: List<TransactionEntity>) {
        deleteAll()
        insertAll(transactions)
    }
}

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}

// Room génère les `actual` sur chaque cible.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
