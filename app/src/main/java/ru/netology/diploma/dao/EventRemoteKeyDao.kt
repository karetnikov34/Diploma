package ru.netology.diploma.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.diploma.entity.EventRemoteKeyEntity

@Dao
interface EventRemoteKeyDao {

    @Query("SELECT MIN(id) FROM EventRemoteKeyEntity")
    suspend fun min(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: EventRemoteKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keys: List<EventRemoteKeyEntity>)

    @Query("DELETE FROM EventRemoteKeyEntity")
    suspend fun removeAll()
}