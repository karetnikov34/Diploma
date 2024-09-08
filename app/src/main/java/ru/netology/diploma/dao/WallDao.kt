package ru.netology.diploma.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.netology.diploma.entity.WallEntity

@Dao
interface WallDao {

    @Query("SELECT * FROM WallEntity ORDER BY id DESC")
    fun getAll(): Flow<List<WallEntity>>  // Правильный импорт д.б. - kotlinx.coroutines.flow.Flow !!!

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wall: List<WallEntity>)

    @Query("DELETE FROM WallEntity")
    suspend fun deleteAll()

    @Transaction
    suspend fun updateWall (wall: List<WallEntity>) {
        deleteAll()
        insert(wall)
    }


    @Query("UPDATE WallEntity SET isPlaying = :isPlaying")
    fun updatePlayerWall (isPlaying: Boolean)

    @Query("UPDATE WallEntity SET isPlaying = :isPlaying WHERE id = :postId")
    suspend fun updateIsPlayingWall (postId: Int, isPlaying: Boolean)

}