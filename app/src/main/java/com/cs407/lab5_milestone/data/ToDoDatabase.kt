package com.cs407.lab5_milestone.data

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.*
import com.cs407.lab5_milestone.R
import java.util.Date


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


//UserNoteRelation
@Entity(
    primaryKeys = ["userId", "taskId"],
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = TodoItem::class,
        parentColumns = ["taskId"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["taskId"])]
)
data class UserTodoRelation(
    val userId: Int,
    val taskId: Int
)

// Entity representing a to-do item
@Entity
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val taskId: Int = 0,
    val taskTitle: String,
    val taskDescription: String,
    val dueDate: Date,
    val priority: Int,
    val lastEdited: Date
)

data class TaskSummary(
    @ColumnInfo(name = "taskId") val taskId: Int,
    @ColumnInfo(name = "taskTitle") val taskTitle: String,
    @ColumnInfo(name = "taskDescription") val taskDescription: String,
    @ColumnInfo(name = "dueDate") val dueDate: Date,
    @ColumnInfo(name = "priority") val priority: Int,
    @ColumnInfo(name = "lastEdited") val lastEdited: Date
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
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT * FROM User, TodoItem, UserTodoRelation
        WHERE User.userId = :id
        AND UserTodoRelation.userId = User.userId
        AND TodoItem.taskId = UserTodoRelation.taskId
        ORDER BY TodoItem.lastEdited DESC
    """)
    suspend fun getUsersWithTodoListsById(id: Int): List<TaskSummary>

    // Same query but returns a PagingSource for pagination
    @Query("""
        SELECT * FROM User, TodoItem, UserTodoRelation
        WHERE User.userId = :id
        AND UserTodoRelation.userId = User.userId
        AND TodoItem.taskId = UserTodoRelation.taskId
        ORDER BY TodoItem.lastEdited DESC
    """)
    fun getUsersWithTodoListsByIdPaged(id: Int): PagingSource<Int, TaskSummary>

    // Insert a new user into the database
    @Insert(entity = User::class)
    suspend fun insert(user: User)
}


// DAO for interacting with the TodoItem entity
@Dao
interface TodoDao {

    @Query("SELECT * FROM TodoItem WHERE taskId = :id")
    suspend fun getById(id: Int): TodoItem

    @Query("SELECT taskId FROM TodoItem WHERE rowid = :rowId")
    suspend fun getByRowId(rowId: Long): Int

    // Insert or update a task (upsert operation)
    @Upsert(entity = TodoItem::class)
    suspend fun upsert(todoItem: TodoItem): Long

    // Insert a relation between a user and a note
    @Insert
    suspend fun insertRelation(userAndTodo: UserTodoRelation)

    @Transaction
    suspend fun upsertTodo(todoItem: TodoItem, userId: Int) {
        val rowId = upsert(todoItem)
        if (todoItem.taskId == 0) { // New note
            val taskId = getByRowId(rowId)
            insertRelation(UserTodoRelation(userId, taskId))
        }
    }
    //@Query("SELECT * FROM TodoItem ORDER BY priority DESC, dueDate ASC")
    //fun getAllTodos(): PagingSource<Int, TodoItem>

    // Query to count the number of to-do a user has
    @Query("""
        SELECT COUNT(*) FROM User, TodoItem, UserTodoRelation
        WHERE User.userId = :userId
        AND UserTodoRelation.userId = User.userId
        AND TodoItem.taskId = UserTodoRelation.taskId
    """)
    suspend fun userTodoCount(userId: Int): Int
}

@Dao
interface DeleteDao {

    // Delete a user by their userId
    @Query("DELETE FROM user WHERE userId = :userId")
    suspend fun deleteUser(userId: Int)

    // Query to get all note IDs related to a user
    @Query("""
        SELECT TodoItem.taskId FROM User, TodoItem, UserTodoRelation
        WHERE User.userId = :userId
        AND UserTodoRelation.userId = User.userId
        AND TodoItem.taskId = UserTodoRelation.taskId
    """)
    suspend fun getALLTaskIdsByUser(userId: Int): List<Int>

    // Delete notes by their IDs
    @Query("DELETE FROM todoItem WHERE taskId IN (:taskIds)")
    suspend fun deleteTodos(taskIds: List<Int>)

    // Transaction to delete a user and all their notes
    @Transaction
    suspend fun delete(userId: Int) {
        deleteTodos(getALLTaskIdsByUser(userId))
        deleteUser(userId)
    }
}


@Database(entities = [User::class, TodoItem::class, UserTodoRelation::class], version = 1, exportSchema = false)
// Database class with all entities and DAOs
@TypeConverters(Converters::class)
abstract class ToDoDatabase : RoomDatabase() {
    // Provide DAOs to access the database
    abstract fun userDao(): UserDao
    abstract fun todoDao(): TodoDao
    abstract fun deleteDao(): DeleteDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: ToDoDatabase? = null

        // Get or create the database instance
        fun getDatabase(context: Context): ToDoDatabase {
            // If the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(lock = this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ToDoDatabase::class.java,
                    context.getString(R.string.todo_database) // Database name from resources
                ).build()
                INSTANCE = instance
                // Return instance
                instance
            }
        }
    }
}

