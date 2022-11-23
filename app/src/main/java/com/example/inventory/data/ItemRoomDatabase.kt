package com.example.inventory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.inventory.SQLCipherUtils
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.IOException


@Database(entities = [Item::class], version = 3, exportSchema = false)
abstract class ItemRoomDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    companion object {
        @Volatile
        private var INSTANCE: ItemRoomDatabase? = null
        fun getDatabase(context: Context, passphrase: String): ItemRoomDatabase {
            val namedb = "item_database"
            return INSTANCE ?: synchronized(this) {
                val dbFile = context.getDatabasePath(namedb)
                val password = passphrase.toByteArray()
                val state = SQLCipherUtils.getDatabaseState(context, dbFile)

                if (state == SQLCipherUtils.State.UNENCRYPTED) {
                    val dbTemp = context.getDatabasePath("_temp.db")

                    dbTemp.delete()

                    SQLCipherUtils.encryptTo(context, dbFile, dbTemp, password)

                    val dbBackup = context.getDatabasePath("_backup.db")

                    if (dbFile.renameTo(dbBackup)) {
                        if (dbTemp.renameTo(dbFile)) {
                            dbBackup.delete()
                        } else {
                            dbBackup.renameTo(dbFile)
                            throw IOException("Could not rename $dbTemp to $dbFile")
                        }
                    } else {
                        dbTemp.delete()
                        throw IOException("Could not rename $dbFile to $dbBackup")
                    }
                }

                val supportFactory = SupportFactory(password)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ItemRoomDatabase::class.java,
                    namedb,
                )
                    .openHelperFactory(supportFactory)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}