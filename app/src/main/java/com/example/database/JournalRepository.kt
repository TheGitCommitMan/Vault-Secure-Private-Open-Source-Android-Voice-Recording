package com.example.database

import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    val allEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()

    suspend fun insert(entry: JournalEntry) {
        journalDao.insertEntry(entry)
    }

    suspend fun deleteById(id: Int) {
        journalDao.deleteEntryById(id)
    }
}
