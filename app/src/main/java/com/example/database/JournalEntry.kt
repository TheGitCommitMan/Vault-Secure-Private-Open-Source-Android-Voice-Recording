package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val encryptedText: ByteArray,
    val iv: ByteArray,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSec: Int = 0,
    val category: String = "Personal"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as JournalEntry
        if (id != other.id) return false
        if (!encryptedText.contentEquals(other.encryptedText)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (timestamp != other.timestamp) return false
        if (durationSec != other.durationSec) return false
        if (category != other.category) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + encryptedText.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + durationSec
        result = 31 * result + category.hashCode()
        return result
    }
}
