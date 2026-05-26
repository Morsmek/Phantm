package com.morsmek.phantm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.morsmek.phantm.db.AppDatabase

class AutoDeleteWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val settings = db.identityDao().getIdentityOnce()

            val autoDeleteDays = settings?.autoDeleteDays ?: 0
            if (autoDeleteDays > 0) {
                val cutoffTime = System.currentTimeMillis() - (autoDeleteDays * 24L * 60L * 60L * 1000L)
                db.messageDao().deleteOldMessages(cutoffTime)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
