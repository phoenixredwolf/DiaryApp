package com.phoenixredwolf.mongo.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.phoenixredwolf.mongo.database.entity.ImageToDelete
import com.phoenixredwolf.mongo.database.entity.ImageToUpload

@Database(
    entities = [ImageToUpload::class, ImageToDelete::class],
    version = 2,
    exportSchema = false
)
abstract class ImagesDatabase: RoomDatabase() {
    abstract fun imageToUploadDao(): ImageToUploadDao
    abstract fun imageToDeleteDao(): ImageToDeleteDao
}