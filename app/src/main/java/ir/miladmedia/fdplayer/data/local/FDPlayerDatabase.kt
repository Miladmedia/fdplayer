package ir.miladmedia.fdplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ir.miladmedia.fdplayer.data.local.dao.BluetoothRuleDao
import ir.miladmedia.fdplayer.data.local.dao.PlaylistDao
import ir.miladmedia.fdplayer.data.local.entity.BluetoothRuleEntity
import ir.miladmedia.fdplayer.data.local.entity.PlaylistEntity

@Database(
    entities = [
        PlaylistEntity::class,
        BluetoothRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FDPlayerDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun bluetoothRuleDao(): BluetoothRuleDao
}
