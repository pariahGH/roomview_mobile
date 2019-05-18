package tech.teksavvy.roomviewmobile

import android.content.Context
import androidx.room.*
import com.univocity.parsers.annotations.Parsed
import java.io.Serializable
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.*
import androidx.room.Query

@Entity(tableName = "rooms")
class RoomItem: Serializable{
    @ColumnInfo(name = "ip")
    @Parsed
    var ip:String = ""
    @PrimaryKey
    @ColumnInfo(name = "room_number")
    @Parsed
    var room:String = ""
}

@Database(entities = [RoomItem::class], version = 1, exportSchema = false)
abstract class RoomViewDb : RoomDatabase() {

    abstract fun roomDao(): RoomDao

    companion object {

        @Volatile private var INSTANCE: RoomViewDb? = null

        fun getInstance(context: Context): RoomViewDb =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
                Room.databaseBuilder(context.applicationContext,
                        RoomViewDb::class.java, "roomview.db")
                        .build()
    }
}

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms WHERE room_number = :id")
    fun getRoonmById(id: String): List<RoomItem>
    @Query("SELECT * FROM rooms")
    fun getRooms(): List<RoomItem>
    @Insert(onConflict = REPLACE)
    fun insertRoom(room: RoomItem)
    @Query("DELETE FROM Rooms")
    fun deleteAllRooms()
}
