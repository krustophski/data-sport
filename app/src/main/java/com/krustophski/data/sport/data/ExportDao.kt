package com.krustophski.data.sport.data

import androidx.room.*

@Dao
interface ExportDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun save(export: Export): Long

    @Update
    suspend fun update(export: Export)

    @Query("SELECT * FROM Export")
    suspend fun load(): Array<Export>

    @Query("SELECT uri FROM Export")
    suspend fun getUris(): Array<String>

    @Delete(entity = Export::class)
    suspend fun delete(exports: Array<Export>)
}
