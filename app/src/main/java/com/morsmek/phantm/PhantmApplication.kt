package com.morsmek.phantm

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class PhantmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
            
        val autoDeleteRequest = PeriodicWorkRequestBuilder<AutoDeleteWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AutoDeleteWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            autoDeleteRequest
        )
    }
}
