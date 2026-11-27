package ir.miladmedia.fdplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.miladmedia.fdplayer.data.local.entity.BluetoothRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BluetoothRuleDao {
    @Query("SELECT * FROM bluetooth_rules ORDER BY id DESC")
    fun observeAll(): Flow<List<BluetoothRuleEntity>>

    @Query("SELECT * FROM bluetooth_rules WHERE status = 'ENABLED' ORDER BY id DESC")
    suspend fun getActiveRules(): List<BluetoothRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BluetoothRuleEntity): Long

    @Query("DELETE FROM bluetooth_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE bluetooth_rules SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
