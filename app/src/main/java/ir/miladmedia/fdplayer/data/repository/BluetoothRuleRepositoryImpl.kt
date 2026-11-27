package ir.miladmedia.fdplayer.data.repository

import ir.miladmedia.fdplayer.data.local.dao.BluetoothRuleDao
import ir.miladmedia.fdplayer.data.mapper.toDomain
import ir.miladmedia.fdplayer.data.mapper.toEntity
import ir.miladmedia.fdplayer.domain.model.BluetoothRule
import ir.miladmedia.fdplayer.domain.repository.BluetoothRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothRuleRepositoryImpl @Inject constructor(
    private val dao: BluetoothRuleDao
) : BluetoothRuleRepository {

    override fun observeRules(): Flow<List<BluetoothRule>> {
        return dao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getActiveRules(): List<BluetoothRule> {
        return dao.getActiveRules().map { it.toDomain() }
    }

    override suspend fun saveRule(rule: BluetoothRule): Long {
        return dao.upsert(rule.toEntity())
    }

    override suspend fun deleteRule(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun toggleRule(id: Long, enabled: Boolean) {
        dao.updateStatus(id, if (enabled) "ENABLED" else "DISABLED")
    }
}
