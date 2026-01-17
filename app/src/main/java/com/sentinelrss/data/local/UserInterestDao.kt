package com.sentinelrss.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserInterestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterest(interest: UserInterest)

    @Query("SELECT * FROM user_interests")
    suspend fun getAllInterests(): List<UserInterest>
}
