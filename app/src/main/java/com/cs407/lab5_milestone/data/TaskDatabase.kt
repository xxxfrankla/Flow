package com.cs407.lab5_milestone.data

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
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
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cs407.lab5_milestone.R
import java.util.Date

// Define your own @Entity, @Dao and @Database
// Define your own @Entity, @Dao and @Database
//User Entity with a unique ID on user name
@Entity(
    indices = [Index(value = ["userName"], unique = true
    )]
)
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0,
    val userName: String = ""
)

// Converter class to handle Date <-> Long type conversions
class Converters {
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
data class Task(
    @PrimaryKey(autoGenerate = true) val taskId: Int = 0, //auto-generated primary key for Task
    val taskTitle: String, //Title of the task
    val taskAbstract: String, // Short summary of the task
    //Detailed content of the note (optional, might be null)
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT) val taskDetail: String?,
    val taskPath: String?,
    val lastEdited: Date,
    val priority: Int,
    val dueDate: Date?,
    val estimatedTime: Int
)

//UserNoteRelation
@Entity(
    primaryKeys = ["userId", "taskId"],
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Task::class,
        parentColumns = ["taskId"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class UserTaskRelation(
    val userId: Int,
    val taskId: Int
)
//Summary projection of the Note entity
// A summary projection of the Note entity, for displaying limited fields in queries
data class TaskSummary(
    val taskId: Int,
    val taskTitle: String,
    val taskAbstract: String,
    val lastEdited: Date,
    val dueDate: Date?,
    val priority: Int

)

//DAO for interacting with the User Entity
// DAO (Data Access Object) for interacting with the User entity in the database
@Dao
interface UserDao {

    // Query to get a User by their userName
    @Query("SELECT * FROM user WHERE userName = :name")
    suspend fun getByName(name: String): User

    // Query to get a User by their userId
    @Query("SELECT * FROM user WHERE userId = :id")
    suspend fun getById(id: Int): User

    // Query to get a list of NoteSummary for a user, ordered by lastEdited
    @Query(
        """
        SELECT * FROM User, Task, UserTaskRelation
        WHERE User.userId = :id
        AND UserTaskRelation.userId = User.userId
        AND Task.taskId = UserTaskRelation.taskId
        ORDER BY Task.lastEdited DESC
    """
    )
    suspend fun getUsersWithTaskListsById(id: Int): List<TaskSummary>

    // Same query but returns a PagingSource for pagination
    @Query(
        """
    SELECT * FROM Task
    INNER JOIN UserTaskRelation ON Task.taskId = UserTaskRelation.taskId
    WHERE UserTaskRelation.userId = :id
    ORDER BY Task.dueDate ASC, Task.priority DESC, Task.estimatedTime DESC
    """
    )
    fun getUsersWithTaskListsByIdPaged(id: Int): PagingSource<Int, TaskSummary>

    // Insert a new user into the database
    @Insert(entity = User::class)
    suspend fun insert(user: User)
}

// DAO for interacting with the Task entity
@Dao
interface TaskDao {

    // Query to get a task by its taskId
    @Query("SELECT * FROM task WHERE taskId = :id")
    suspend fun getById(id: Int): Task

    // Query to get a task's ID by its rowId (SQLite internal ID)
    @Query("SELECT taskId FROM task WHERE rowid = :rowId")
    suspend fun getByRowId(rowId: Long): Int

    // Insert or update a task (upsert operation)
    @Upsert(entity = Task::class)
    suspend fun upsert(task: Task): Long

    // Insert a relation between a user and a note
    @Insert
    suspend fun insertRelation(userAndTask: UserTaskRelation)

    // Insert or update a Task and create a relation to the User if it's a new Task
    @Transaction
    suspend fun upsertTask(task: Task, userId: Int) {
        val rowId = upsert(task)
        if (task.taskId == 0) { // New note
            val taskId = getByRowId(rowId)
            insertRelation(UserTaskRelation(userId, taskId))
        }
    }

    // Query to count the number of notes a user has
    @Query(
        """
        SELECT COUNT(*) FROM User, Task, UserTaskRelation
        WHERE User.userId = :userId
        AND UserTaskRelation.userId = User.userId
        AND Task.taskId = UserTaskRelation.taskId
    """
    )
    suspend fun userTaskCount(userId: Int): Int

    @Query("SELECT * FROM Task ORDER BY dueDate ASC")
    suspend fun getTasksOrderedByDueDate(): List<Task>

    @Query("SELECT * FROM Task WHERE dueDate < :currentDate")
    suspend fun getOverdueTasks(currentDate: Long): List<Task>

    @Query("SELECT * FROM Task WHERE dueDate BETWEEN :startDate AND :endDate")
    suspend fun getTasksInRange(startDate: Long, endDate: Long): List<Task>

    @Query("SELECT * FROM Task WHERE priority >= :minPriority ORDER BY priority DESC")
    suspend fun getHighPriorityTasks(minPriority: Int): List<Task>

    @Query("SELECT * FROM Task WHERE estimatedTime <= :maxTime ORDER BY estimatedTime ASC")
    suspend fun getTasksWithEstimatedTime(maxTime: Int): List<Task>


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
interface DeleteDao {

    // Delete a user by their userId
    @Query("DELETE FROM user WHERE userId = :userId")
    suspend fun deleteUser(userId: Int)

    // Query to get all note IDs related to a user
    @Query(
        """
        SELECT Task.taskId FROM User, Task, UserTaskRelation
        WHERE User.userId = :userId
        AND UserTaskRelation.userId = User.userId
        AND Task.taskId = UserTaskRelation.taskId
    """
    )
    suspend fun getALLTaskIdsByUser(userId: Int): List<Int>

    // Delete notes by their IDs
    @Query("DELETE FROM task WHERE taskId IN (:tasksIds)")
    suspend fun deleteTasks(tasksIds: List<Int>)

    // Transaction to delete a user and all their notes
    @Transaction
    suspend fun delete(userId: Int) {
        deleteTasks(getALLTaskIdsByUser(userId))
        deleteUser(userId)
    }
}

// Database class with all entities and DAOs
@Database(entities = [User::class, Task::class, UserTaskRelation::class], version = 2)
// Database class with all entities and DAOs
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    // Provide DAOs to access the database
    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun deleteDao(): DeleteDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        // Get or create the database instance
        fun getDatabase(context: Context): TaskDatabase {
            // If the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(lock = this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    context.getString(R.string.task_database) // Database name from resources
                )
                .build()
                INSTANCE = instance
                // Return instance
                instance
            }
        }
    }
}
fun resetDatabase(context: Context) {
    context.deleteDatabase("taskDatabase")
}
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Task ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE Task ADD COLUMN estimatedTime INTEGER")
        database.execSQL("ALTER TABLE Task ADD COLUMN dueDate INTEGER")
    }
}

