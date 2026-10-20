package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SafetyLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemAppDao {

    @Query(
        """
        SELECT * FROM system_applications 
        ORDER BY is_installed DESC, 
                 CASE safety_level 
                     WHEN 'SAFE' THEN 0 
                     WHEN 'CAUTION' THEN 1 
                     ELSE 2 
                 END ASC, 
                 display_name ASC
        """
    )
    fun observeAllSystemApps(): Flow<List<SystemAppEntity>>

    @Query("SELECT * FROM system_applications WHERE is_installed = 1 ORDER BY display_name ASC")
    fun observeInstalledSystemApps(): Flow<List<SystemAppEntity>>

    @Query("SELECT * FROM system_applications WHERE safety_level = :riskLevel ORDER BY display_name ASC")
    fun observeAppsByRiskLevel(riskLevel: SafetyLevel): Flow<List<SystemAppEntity>>

    @Query("SELECT * FROM system_applications WHERE is_enabled = :isEnabled ORDER BY display_name ASC")
    fun observeAppsByEnabledState(isEnabled: Boolean): Flow<List<SystemAppEntity>>

    @Query("SELECT * FROM system_applications WHERE is_marked_for_debloat = 1 ORDER BY display_name ASC")
    fun observeSelectedForDebloat(): Flow<List<SystemAppEntity>>

    @Query("SELECT * FROM system_applications")
    suspend fun getAllAppsOneShot(): List<SystemAppEntity>

    @Query("SELECT * FROM system_applications WHERE package_name = :packageName LIMIT 1")
    suspend fun getAppByPackageName(packageName: String): SystemAppEntity?

    @Query("SELECT COUNT(*) FROM system_applications")
    fun observeTotalStoredCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<SystemAppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApp(app: SystemAppEntity)

    @Update
    suspend fun updateApp(app: SystemAppEntity)

    @Query("UPDATE system_applications SET is_enabled = :isEnabled, last_scanned_at = :timestamp WHERE package_name = :packageName")
    suspend fun updateEnabledState(
        packageName: String,
        isEnabled: Boolean,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE system_applications SET safety_level = :riskLevel WHERE package_name = :packageName")
    suspend fun updateRiskLevel(packageName: String, riskLevel: SafetyLevel)

    @Query("UPDATE system_applications SET is_marked_for_debloat = :isMarked WHERE package_name = :packageName")
    suspend fun updateSelectionState(packageName: String, isMarked: Boolean)

    @Query("UPDATE system_applications SET is_marked_for_debloat = 1 WHERE package_name IN (:packageNames) AND safety_level != 'ESSENTIAL'")
    suspend fun markPackagesForDebloat(packageNames: List<String>)

    @Query("UPDATE system_applications SET is_marked_for_debloat = 0")
    suspend fun clearAllSelections()

    @Query("DELETE FROM system_applications WHERE package_name = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("DELETE FROM system_applications")
    suspend fun deleteAll()
}
