package ru.netology.diploma.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.diploma.entity.UserEntity

@Dao
interface UserDao {

    @Query("SELECT * FROM UserEntity ORDER BY id DESC")
    fun getAll(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(users: List<UserEntity>)

    @Query("UPDATE UserEntity SET isSelected = :isSelected WHERE id = :id")
    fun updateUsers(id: Int, isSelected: Boolean)

    @Query("UPDATE UserEntity SET isSelected = :isSelected")
    suspend fun deselectUsers(isSelected: Boolean)
}