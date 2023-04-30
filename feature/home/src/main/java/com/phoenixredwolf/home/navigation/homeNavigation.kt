package com.phoenixredwolf.home.navigation

import android.widget.Toast
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.phoenixredwolf.home.HomeScreen
import com.phoenixredwolf.home.HomeViewModel
import com.phoenixredwolf.ui.components.DisplayAlertDialog
import com.phoenixredwolf.util.APP_ID
import com.phoenixredwolf.util.Screen
import com.phoenixredwolf.util.model.RequestState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
