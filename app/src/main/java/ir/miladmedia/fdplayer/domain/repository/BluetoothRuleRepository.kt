package ir.miladmedia.fdplayer.domain.repository

import ir.miladmedia.fdplayer.domain.model.BluetoothRule
import kotlinx.coroutines.flow.Flow

interface BluetoothRuleRepository {
    fun observeRules(): Flow<List<BluetoothRule>>
    suspend fun getActiveRules(): List<BluetoothRule>
    suspend fun saveRule(rule: BluetoothRule): Long
    suspend fun deleteRule(id: Long)
    suspend fun toggleRule(id: Long, enabled: Boolean)
}
