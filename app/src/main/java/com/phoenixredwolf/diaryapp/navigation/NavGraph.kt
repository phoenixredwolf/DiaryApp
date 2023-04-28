package com.phoenixredwolf.diaryapp.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.phoenixredwolf.diaryapp.model.Mood
import com.phoenixredwolf.diaryapp.model.RequestState
import com.phoenixredwolf.diaryapp.presentation.components.DisplayAlertDialog
import com.phoenixredwolf.diaryapp.presentation.screens.auth.AuthenticationScreen
import com.phoenixredwolf.diaryapp.presentation.screens.auth.AuthenticationViewModel
import com.phoenixredwolf.diaryapp.presentation.screens.home.HomeScreen
import com.phoenixredwolf.diaryapp.presentation.screens.home.HomeViewModel
import com.phoenixredwolf.diaryapp.presentation.screens.write.WriteScreen
import com.phoenixredwolf.diaryapp.presentation.screens.write.WriteViewModel
import com.phoenixredwolf.diaryapp.util.APP_ID
import com.phoenixredwolf.diaryapp.util.WRITE_SCREEN_ARGUMENT_KEY
import com.stevdzasan.messagebar.rememberMessageBarState
import com.stevdzasan.onetap.rememberOneTapSignInState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SetupNavGraph(
    startDestination: String,
    navController: NavHostController,
    onDataLoaded: () -> Unit
) {
    val context = LocalContext.current
    NavHost(
        startDestination = startDestination,
        navController = navController
    ) {
        authenticationRoute(
            navigateToHome = {
                navController.popBackStack()
                navController.navigate(Screen.Home.route)
                Log.d("Navigate", "Navigate  to Home")
            },
            onDataLoaded = onDataLoaded
        )
        homeRoute(
            navigateToWrite = {
                navController.navigate(Screen.Write.route)
            },
            navigateToAuth = {
                navController.popBackStack()
                navController.navigate(Screen.Authentication.route)
            },
            onDataLoaded = onDataLoaded,
            navigateToWriteWithArgs = {
                navController.navigate(Screen.Write.passDiaryId(diaryId = it))
            }
        )
        writeRoute(
            onBackPressed = {
                navController.popBackStack()
            }
        )
    }
}

fun NavGraphBuilder.authenticationRoute(
    navigateToHome: () -> Unit,
    onDataLoaded: () -> Unit
) {
    composable(route = Screen.Authentication.route) {
        val viewModel: AuthenticationViewModel = viewModel()
        val authenticated by viewModel.authenticated
        val loadingState by viewModel.loadingState
        val oneTapState = rememberOneTapSignInState()
        val messageBarState = rememberMessageBarState()

        LaunchedEffect(key1 = Unit) {
            onDataLoaded()
        }
        AuthenticationScreen(
            authenticated = authenticated,
            loadingState = loadingState,
            onButtonClicked = {
                oneTapState.open()
                viewModel.setLoading(true)
            },
            oneTapState = oneTapState,
            messageBarState = messageBarState,
            onSuccessfulFirebaseSignIn = { tokenId ->
                viewModel.signInWithMongAtlas(
                    tokenId = tokenId,
                    onSuccess = {
                        messageBarState.addSuccess("Successfully Authenticated")
                        viewModel.setLoading(false)
                    },
                    onError = {
                        messageBarState.addError(it)
                        viewModel.setLoading(false)
                    })
            },
            onDialogDismissed = { message ->
                messageBarState.addError(Exception(message))
                viewModel.setLoading(false)
            },
            onFailedFirebaseSignIn = {
                messageBarState.addError(it)
                viewModel.setLoading(false)
            },
            navigateToHome = navigateToHome
        )
    }
}

fun NavGraphBuilder.homeRoute(
    navigateToWrite: () -> Unit,
    navigateToWriteWithArgs: (String) -> Unit,
    navigateToAuth: () -> Unit,
    onDataLoaded: () -> Unit
) {
    composable(route = Screen.Home.route) {
        val viewModel: HomeViewModel = hiltViewModel()
        val diaries by viewModel.diaries
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        var signOutDiaglogOpened by remember { mutableStateOf(false) }
        var deleteAllDiaglogOpened by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        LaunchedEffect(key1 = diaries) {
            if (diaries !is RequestState.Loading) {
                onDataLoaded()
            }
        }

        HomeScreen(
            diaries = diaries,
            drawerState = drawerState,
            onMenuClicked = {
                scope.launch {
                    drawerState.open()
                }
            },
            onSignOutClicked = { signOutDiaglogOpened = true },
            onDeleteAllClicked = { deleteAllDiaglogOpened = true },
            navigateToWrite = navigateToWrite,
            navigateToWriteWithArgs = navigateToWriteWithArgs,
            onDateSelected = {
                viewModel.getDiaries(zonedDateTime = it)
            },
            onDateReset = { viewModel.getDiaries() },
            dateIsSelected = viewModel.dateIsSelected
        )
        DisplayAlertDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign out?",
            dialogOpened = signOutDiaglogOpened,
            onDialogClosed = { signOutDiaglogOpened = false },
            onYesClicked = {
                scope.launch(Dispatchers.IO) {
                    val user = App.create(APP_ID).currentUser
                    user?.let {
                        user.logOut()
                        withContext(Dispatchers.Main){
                            navigateToAuth()
                        }
                    }
                }
            }
        )
        DisplayAlertDialog(
            title = "Delete All Diaries",
            message = "Are you sure you want to permanently delete all diaries?",
            dialogOpened = deleteAllDiaglogOpened,
            onDialogClosed = { deleteAllDiaglogOpened = false },
            onYesClicked = {
                scope.launch(Dispatchers.IO) {
                    viewModel.deleteAllDiaries(
                        onSuccess = {
                            Toast.makeText(
                                context,
                                "All diaries delete",
                                Toast.LENGTH_SHORT
                            ).show()
                            scope.launch { drawerState.close() }
                        },
                        onError = {
                            Toast.makeText(
                                context,
                                if(it.message == "No Internet Connection.")
                                    "An internet connection is required for this operation"
                                else it.message,
                                Toast.LENGTH_SHORT
                            ).show()
                            scope.launch { drawerState.close() }}
                    )
                }
            }
        )
    }
}

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