package com.example.notivib.di

import android.content.Context
import com.example.notivib.data.local.RulesDataStore
import com.example.notivib.data.repository.RuleRepositoryImpl
import com.example.notivib.domain.repository.RuleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRulesDataStore(@ApplicationContext context: Context): RulesDataStore {
        return RulesDataStore(context)
    }

    @Provides
    @Singleton
    fun provideRuleRepository(dataStore: RulesDataStore): RuleRepository {
        return RuleRepositoryImpl(dataStore)
    }
}
