package com.cs407.flow

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.*
import java.util.Date

// User Entity: Represents a user with a unique userName
@Entity(
    indices = [Index(value = ["userName"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0,
    val userName: String
)

// Converter for handling Date -> Long (timestamp) and vice versa
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

// Note Entity: Represents a note with title, abstract, optional content, and metadata
@Entity
data class Note(
    @PrimaryKey(autoGenerate = true) val noteId: Int = 0,
    val noteTitle: String,
    val noteAbstract: String,
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) val noteDetail: String?,
    val notePath: String?,
    val lastEdited: Date
)

// Relationship Entity: Links users to their notes
@Entity(
    primaryKeys = ["userId", "noteId"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Note::class,
            parentColumns = ["noteId"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "noteId"])]
)
data class UserNoteRelation(
    val userId: Int,
    val noteId: Int
)

// Summary Projection for Notes
data class NoteSummary(
    val noteId: Int,
    val noteTitle: String,
    val noteAbstract: String,
    val lastEdited: Date
)

// User Data Access Object
@Dao
interface UserDao {
    @Query("SELECT * FROM User WHERE userName = :name")
    suspend fun getByName(name: String): User?

    @Query("SELECT * FROM User WHERE userId = :id")
    suspend fun getById(id: Int): User?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: User): Long
}

// Note Data Access Object
@Dao
interface NoteDao {

    @Query("SELECT * FROM Note WHERE noteId = :id")
    suspend fun getById(id: Int): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Query("DELETE FROM Note WHERE noteId = :id")
    suspend fun deleteById(id: Int)

    @Transaction
    suspend fun upsert(note: Note): Long {
        val rowId = insert(note)
        return rowId
    }

    @Query("""
        SELECT Note.noteId, Note.noteTitle, Note.noteAbstract, Note.lastEdited
        FROM Note
        INNER JOIN UserNoteRelation ON Note.noteId = UserNoteRelation.noteId
        WHERE UserNoteRelation.userId = :userId
        ORDER BY Note.lastEdited DESC
    """)
    fun getNoteSummariesByUserId(userId: Int): PagingSource<Int, NoteSummary>
}

// Deletion DAO for cascade operations
@Dao
interface DeleteDao {
    @Query("DELETE FROM User WHERE userId = :userId")
    suspend fun deleteUserById(userId: Int)

    @Transaction
    suspend fun deleteUserAndNotes(userId: Int) {
        // Ensure all notes associated with the user are deleted
        deleteUserById(userId)
    }
}

// The Room Database
@Database(
    entities = [User::class, Note::class, UserNoteRelation::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao
    abstract fun deleteDao(): DeleteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        // Singleton for the Room database instance
        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database" // Database name
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
