package com.phoenixredwolf.diaryapp.di

import android.content.Context
import androidx.room.Room
import com.phoenixredwolf.util.IMAGES_DATABASE
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): com.phoenixredwolf.mongo.database.ImagesDatabase {
        return Room.databaseBuilder(
            context = context,
            klass = com.phoenixredwolf.mongo.database.ImagesDatabase::class.java,
            name = IMAGES_DATABASE
        ).build()
    }

    @Singleton
    @Provides
    fun provideFirstDao(database: com.phoenixredwolf.mongo.database.ImagesDatabase) = database.imageToUploadDao()

    @Singleton
    @Provides
    fun provideSecondDao(database: com.phoenixredwolf.mongo.database.ImagesDatabase) = database.imageToDeleteDao()

}