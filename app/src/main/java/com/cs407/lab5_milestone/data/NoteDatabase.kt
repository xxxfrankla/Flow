package com.cs407.lab5_milestone.data

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import com.cs407.lab5_milestone.R
import java.util.Date

// Define your own @Entity, @Dao and @Database
// Define your own @Entity, @Dao and @Database
//User Entity with a unique ID on user name
@Entity(
    indices = [Index(value = ["userName"], unique = true
    )]
)
data class U1ser(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0,
    val userName: String = ""
)

// Converter class to handle Date <-> Long type conversions
class Conver1ters {
    @TypeConverter
    fun fromTimestamp(value: Long): Date{
        return Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date): Long{
        return date.time
    }
}

//Note Entity
@Entity
data class Note(
    @PrimaryKey(autoGenerate = true) val noteId: Int = 0, //auto-generated primary key for Note
    val noteTitle: String, //Title of the note
    val noteAbstract: String, // Short summary of the note
    //Detailed content of the note (optional, might be null)
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) val noteDetail: String?,
    val notePath: String?,
    val lastEdited: Date
)

//UserNoteRelation
@Entity(
    primaryKeys = ["userId", "noteId"],
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Note::class,
        parentColumns = ["noteId"],
        childColumns = ["noteId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class UserNoteRelation(
    val userId: Int,
    val noteId: Int
)
//Summary projection of the Note entity
// A summary projection of the Note entity, for displaying limited fields in queries
data class NoteSummary(
    val noteId: Int,
    val noteTitle: String,
    val noteAbstract: String,
    val lastEdited: Date
)

//DAO for interacting with the User Entity
// DAO (Data Access Object) for interacting with the User entity in the database
@Dao
interface User1Dao {

    // Query to get a User by their userName
    @Query("SELECT * FROM user WHERE userName = :name")
    suspend fun getByName(name: String): User

    // Query to get a User by their userId
    @Query("SELECT * FROM user WHERE userId = :id")
    suspend fun getById(id: Int): User

    // Query to get a list of NoteSummary for a user, ordered by lastEdited
    @Query("""
        SELECT * FROM User, Note, UserNoteRelation
        WHERE User.userId = :id
        AND UserNoteRelation.userId = User.userId
        AND Note.noteId = UserNoteRelation.noteId
        ORDER BY Note.lastEdited DESC
    """)
    suspend fun getUsersWithNoteListsById(id: Int): List<NoteSummary>

    // Same query but returns a PagingSource for pagination
    @Query("""
        SELECT * FROM User, Note, UserNoteRelation
        WHERE User.userId = :id
        AND UserNoteRelation.userId = User.userId
        AND Note.noteId = UserNoteRelation.noteId
        ORDER BY Note.lastEdited DESC
    """)
    fun getUsersWithNoteListsByIdPaged(id: Int): PagingSource<Int, NoteSummary>

    // Insert a new user into the database
    @Insert(entity = User::class)
    suspend fun insert(user: User)
}

// DAO for interacting with the Note entity
@Dao
interface NoteDao {

    // Query to get a Note by its noteId
    @Query("SELECT * FROM note WHERE noteId = :id")
    suspend fun getById(id: Int): Note

    // Query to get a Note's ID by its rowId (SQLite internal ID)
    @Query("SELECT noteId FROM note WHERE rowid = :rowId")
    suspend fun getByRowId(rowId: Long): Int

    // Insert or update a Note (upsert operation)
    @Upsert(entity = Note::class)
    suspend fun upsert(note: Note): Long

    // Insert a relation between a user and a note
    @Insert
    suspend fun insertRelation(userAndNote: UserNoteRelation)

    // Insert or update a Note and create a relation to the User if it's a new Note
    @Transaction
    suspend fun upsertNote(note: Note, userId: Int) {
        val rowId = upsert(note)
        if (note.noteId == 0) { // New note
            val noteId = getByRowId(rowId)
            insertRelation(UserNoteRelation(userId, noteId))
        }
    }

    // Query to count the number of notes a user has
    @Query("""
        SELECT COUNT(*) FROM User, Note, UserNoteRelation
        WHERE User.userId = :userId
        AND UserNoteRelation.userId = User.userId
        AND Note.noteId = UserNoteRelation.noteId
    """)
    suspend fun userNoteCount(userId: Int): Int

//    @Query("""
//        SELECT Note.noteId, Note.noteTitle, Note.noteAbstract, Note.lastEdited
//        FROM Note
//        INNER JOIN UserNoteRelation ON Note.noteId = UserNoteRelation.noteId
//        WHERE UserNoteRelation.userId = :userId
//        ORDER BY Note.lastEdited DESC
//    """)
//    suspend fun getUsersWithNoteListsById(userId: Int): List<NoteSummary>
//
//    @Query("DELETE FROM note WHERE noteId = :noteId")
//    suspend fun deleteNoteById(noteId: Int)
}

// DAO for handling deletion of users and their related notes
@Dao
interface Delete1Dao {

    // Delete a user by their userId
    @Query("DELETE FROM user WHERE userId = :userId")
    suspend fun deleteUser(userId: Int)

    // Query to get all note IDs related to a user
    @Query("""
        SELECT Note.noteId FROM User, Note, UserNoteRelation
        WHERE User.userId = :userId
        AND UserNoteRelation.userId = User.userId
        AND Note.noteId = UserNoteRelation.noteId
    """)
    suspend fun getALLNoteIdsByUser(userId: Int): List<Int>

    // Delete notes by their IDs
    @Query("DELETE FROM note WHERE noteId IN (:notesIds)")
    suspend fun deleteNotes(notesIds: List<Int>)

    // Transaction to delete a user and all their notes
    @Transaction
    suspend fun delete(userId: Int) {
        deleteNotes(getALLNoteIdsByUser(userId))
        deleteUser(userId)
    }
}

// Database class with all entities and DAOs
@Database(entities = [User::class, Note::class, UserNoteRelation::class], version = 1)
// Database class with all entities and DAOs
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {
    // Provide DAOs to access the database
    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao
    abstract fun deleteDao(): DeleteDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        // Get or create the database instance
        fun getDatabase(context: Context): NoteDatabase {
            // If the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(lock = this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    context.getString(R.string.note_database) // Database name from resources
                ).build()
                INSTANCE = instance
                // Return instance
                instance
            }
        }
    }
}

