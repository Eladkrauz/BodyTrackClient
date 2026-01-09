////////////////////////////////////////////////////////////
//////////////////// BODY TRACK // CLIENT //////////////////
////////////////////////////////////////////////////////////
//////////////////// FILE: MainActivity ////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client

///////////////
/// IMPORTS ///
///////////////
import android.os.Bundle
import androidx.activity.compose.setContent

///////////////////////////
/// MAIN ACTIVITY CLASS ///
///////////////////////////
/**
 * The main entry point of the BodyTrack application.
 *
 * This activity is responsible for setting up the initial user interface
 * by loading the main Composable content within the app's theme. It serves as
 * the root container for the entire application's UI, which is managed by
 * Jetpack Compose.
 */
class MainActivity : androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            _root_ide_package_.com.bodytrack.client.theme.BodyTrackTheme {
                AppRoot()
            }
        }
    }
}