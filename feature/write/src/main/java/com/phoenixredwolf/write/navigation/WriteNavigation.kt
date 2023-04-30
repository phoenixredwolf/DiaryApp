package com.phoenixredwolf.write.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.phoenixredwolf.write.WriteScreen
import com.phoenixredwolf.write.WriteViewModel
import com.phoenixredwolf.util.WRITE_SCREEN_ARGUMENT_KEY
import com.phoenixredwolf.util.Screen
import com.phoenixredwolf.util.model.Mood

@OptIn(ExperimentalFoundationApi::class)
fun NavGraphBuilder.writeRoute(
    onBackPressed: () -> Unit,
) {
    composable(
        route = Screen.Write.route,
        arguments = listOf(navArgument(name = WRITE_SCREEN_ARGUMENT_KEY) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        })
    ) {
        val viewModel: WriteViewModel = hiltViewModel()
        val uiState = viewModel.uiState
        val galleryState = viewModel.galleryState
        val pagerState = rememberPagerState()
        val pageNumber by remember { derivedStateOf{pagerState.currentPage}}
        val context = LocalContext.current

        WriteScreen(
            onBackPressed = onBackPressed,
            onDeleteClicked = {
                viewModel.deleteDiary(
                    onSuccess = {
                        Log.d("Delete Diary", "Nav Graph onSuccess Entered")
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        Log.d("Delete Diary", "Nav Graph Toast fired")
                        onBackPressed()
                        Log.d("Delete Diary", "Nav Graph onBackPressed")
                    },
                    onError = {message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onTitleChanged = { viewModel.setTitle(title = it) },
            onDescriptionChanged = { viewModel.setDescription(description = it) },
            onSaveClicked = {
                viewModel.upsertDiary(
                    diary = it.apply { mood = Mood.values()[pageNumber].name },
                    onSuccess = { onBackPressed() },
                    onError = {message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onImageSelect = {uri ->
                val type = context.contentResolver.getType(uri)?.split("/")?.last() ?: "jpg"
                viewModel.addImage(image = uri, imageType = type)
            },
            onDateTimeUpdated = { viewModel.updateDateTime(zonedDateTime = it) },
            moodName = { Mood.values()[pageNumber].name},
            pagerState = pagerState,
            uiState = uiState,
            galleryState = galleryState,
            onImageDeleteClicked = { galleryState.removeImage(it) }
        )
    }
}