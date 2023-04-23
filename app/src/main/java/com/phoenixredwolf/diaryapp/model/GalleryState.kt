package com.phoenixredwolf.diaryapp.model

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

@Composable
fun rememberGalleryState(): GalleryState = remember { GalleryState() }

class GalleryState {
    val images = mutableStateListOf<GalleryImage>()
    val imagesToDelete = mutableStateListOf<GalleryImage>()

    fun addImage(galleryImage: GalleryImage) {
        images.add(galleryImage)
    }

    fun removeImage(galleryImage: GalleryImage) {
        images.remove(galleryImage)
        imagesToDelete.add(galleryImage)
    }

    fun clearImagesToBeDeleted() {
        imagesToDelete.clear()
    }
}

data class GalleryImage(
    val image: Uri,
    val remoteImagePath: String = ""
)