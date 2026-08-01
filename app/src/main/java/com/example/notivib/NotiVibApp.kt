package com.example.notivib

import android.app.Application
import com.example.notivib.data.local.RulesDataStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NotiVibApp : Application() {

    @Inject
    lateinit var rulesDataStore: RulesDataStore

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            rulesDataStore.migrateIfNeeded()
        }
    }
}
