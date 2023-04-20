package com.phoenixredwolf.diaryapp.presentation.screens.home

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.phoenixredwolf.diaryapp.R
import com.phoenixredwolf.diaryapp.data.repository.Diaries
import com.phoenixredwolf.diaryapp.util.RequestState

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    diaries: Diaries,
    drawerState: DrawerState,
    onSignOutClicked: () -> Unit,
    onMenuClicked: () -> Unit,
    navigateToWrite: () -> Unit,
    navigateToWriteWithArgs: (String) -> Unit
) {
    var padding by remember { mutableStateOf(PaddingValues()) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    NavigationDrawer(
        drawerState = drawerState, onSignOutClicked = onSignOutClicked
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
            HomeTopBar(scrollBehavior, onMenuClicked)
        }, floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(
                    end = padding.calculateEndPadding(
                        LayoutDirection.Ltr
                    )
                ), onClick = navigateToWrite
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "New Diary Icon")
            }
        }, content = {
            padding = it
            when (diaries) {
                is RequestState.Success -> {
                    Log.d("Diary data", "From HomeScreen Success -> ${diaries.data}")
                    HomeContent(paddingValues = it, diaryNotes = diaries.data, onClick = navigateToWriteWithArgs)
                }

                is RequestState.Error -> {
                    Log.d("Diary data", "From HomeScreen Error - ${diaries.error.message}")
                    EmptyPage(title = "Error", subtitle = "${diaries.error.message}")
                }

                is RequestState.Loading -> {
                    Log.d("Diary data", "From HomeScreen Loading")
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                else -> {}
            }
        })
    }
}

@Composable
fun NavigationDrawer(
    drawerState: DrawerState, onSignOutClicked: () -> Unit, content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState, drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(250.dp), content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = Modifier.size(250.dp),
                        painter = painterResource(id = R.drawable.mileagelogo),
                        contentDescription = "Logo Image"
                    )
                }
                NavigationDrawerItem(label = {
                    Row(modifier = Modifier.padding(horizontal = 12.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.google_logo),
                            contentDescription = "Google Logo"
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Sign Out")
                    }
                }, selected = false, onClick = onSignOutClicked)

            })
        }, content = content
    )
}