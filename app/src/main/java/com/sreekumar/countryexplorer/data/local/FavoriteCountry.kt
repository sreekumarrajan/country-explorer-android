package com.sreekumar.countryexplorer.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * ## Room in three declarations
 *
 * Room is Android's SQLite layer: you declare tables as classes and queries as
 * interface methods, and it generates type-safe, compile-time-checked code.
 * The iOS repo does the same job with SwiftData.
 *
 * The genuinely important difference from web localStorage: Room checks your
 * SQL *at build time*. Misspell a column and the build fails, not production.
 */

/** One row of the `favorite_countries` table. */
@Entity(tableName = "favorite_countries")
data class FavoriteCountryEntity(
    @PrimaryKey val countryCode: String,
)

/**
 * DAO = Data Access Object: the queries. Room writes the implementation.
 */
@Dao
interface FavoriteCountryDao {

    /**
     * Returns a [Flow] — a *stream* of results, not a one-time read. Room
     * re-runs this query and emits a new list every time the table changes.
     *
     *   RxJS:    Observable<string[]>  (and this is a hot, replaying stream)
     *   Kotlin:  Flow<List<String>>
     *
     * This is the single most useful idea in the data layer: favouriting a
     * country writes one row, and every screen observing this Flow updates
     * itself. No event bus, no manual refresh, no cache invalidation.
     *
     * Note it is NOT `suspend`: you are not awaiting one value, you are getting
     * a stream to collect from.
     */
    @Query("SELECT countryCode FROM favorite_countries")
    fun observeFavoriteCodes(): Flow<List<String>>

    /** IGNORE = inserting a duplicate is a no-op rather than an error. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteCountryEntity)

    // `:code` binds the function parameter into the SQL — parameterised, so
    // SQL injection is not a thing here.
    @Query("DELETE FROM favorite_countries WHERE countryCode = :code")
    suspend fun delete(code: String)
}

/**
 * The database itself. `abstract` because Room generates the subclass.
 *
 * `version = 1`: bump this whenever the schema changes, and supply a Migration
 * so existing users don't lose data. (Shipping a schema change without one
 * crashes the app on upgrade — a classic first-release mistake.)
 */
// exportSchema = false: Room can write a JSON snapshot of your schema to disk
// so migrations can be verified in tests. Valuable for a shipping app with real
// user data; unnecessary ceremony for one table of favourites. Turning it off
// explicitly silences the build warning rather than ignoring it.
@Database(
    entities = [FavoriteCountryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CountryExplorerDatabase : RoomDatabase() {
    abstract fun favoriteCountryDao(): FavoriteCountryDao
}
