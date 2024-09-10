package ru.netology.diploma.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.netology.diploma.entity.JobEntity

@Dao
interface JobDao {

    @Query("SELECT * FROM JobEntity ORDER BY id DESC")
    fun getJobs(): Flow<List<JobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(jobs: List<JobEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: JobEntity)

    @Query("DELETE FROM JobEntity")
    suspend fun deleteAll()

    @Transaction
    suspend fun updateJobs(jobs: List<JobEntity>) {
        deleteAll()
        insert(jobs)
    }

    @Query("DELETE FROM JobEntity WHERE id = :id")
    suspend fun removeJobById(id: Int)
}