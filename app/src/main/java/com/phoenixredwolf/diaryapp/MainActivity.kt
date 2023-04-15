package com.phoenixredwolf.diaryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.phoenixredwolf.diaryapp.data.repository.MongoDB
import com.phoenixredwolf.diaryapp.navigation.Screen
import com.phoenixredwolf.diaryapp.navigation.SetupNavGraph
import com.phoenixredwolf.diaryapp.ui.theme.DiaryAppTheme
import com.phoenixredwolf.diaryapp.util.APP_ID
import io.realm.kotlin.mongodb.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        MongoDB.configureRealm()
        setContent {
            DiaryAppTheme {
                val navController = rememberNavController()
                SetupNavGraph(
                    startDestination = getStartDestination(),
                    navController = navController
                )

            }
        }
    }
}

private fun getStartDestination(): String {
    val user = App.Companion.create(APP_ID).currentUser
    return if(user != null && user.loggedIn) Screen.Home.route else Screen.Authentication.route
}