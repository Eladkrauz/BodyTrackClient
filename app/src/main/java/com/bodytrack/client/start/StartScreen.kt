////////////////////////////////////////////////////////////
/////////////// BODY TRACK // CLIENT // START //////////////
////////////////////////////////////////////////////////////
///////////////////// FILE: StartScreen ////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.start

///////////////
/// IMPORTS ///
///////////////
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.bodytrack.client.R

////////////////////////////////
/// START SCREEN COMPOSABLE ///
///////////////////////////////
/**
 * The entry screen of the BodyTrack application, displayed to the user on first launch.
 *
 * This composable serves as a welcoming splash screen. It features the app's logo,
 * a title, and an animated carousel of motivational quotes. The main purpose is to
 * provide a visually appealing entry point and a clear call-to-action to proceed
 * into the main application.
 *
 * Key UI elements include:
 * - **App Logo**: A large, centrally displayed logo for branding.
 * - **Animated Quote Carousel**: A box that cycles through a list of motivational fitness quotes every few seconds with a vertical slide animation. This state is managed locally within the composable.
 * - **Start Button**: The primary interactive element. Pressing this button triggers the `onStartClicked` callback, signaling the user's intent to enter the app.
 *
 * The screen follows a stateless design pattern concerning navigation. It does not decide
 * what happens next; it only reports the "start" event to its parent composable,
 * which is responsible for handling the navigation logic.
 *
 * @param onStartClicked A lambda function that is invoked when the user clicks the "Start" button.
 *                       This callback is used to notify the parent composable to handle navigation
 *                       to the next screen.
 */
@Composable
fun StartScreen(
    onStartClicked: () -> Unit
) {

    ////////////////////
    /// QUOTES STATE ///
    ////////////////////
    /*
     * Static list of motivational quotes.
     */
    val quotes = listOf(
        "Train smart, not hard",
        "Consistency beats intensity",
        "Form matters more than weight",
        "Your body remembers what you teach it"
    )
    var quoteIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            quoteIndex = (quoteIndex + 1) % quotes.size
        }
    }

    //////////////////////
    /// SCREEN CONTENT ///
    //////////////////////
    /*
     * Column is the main vertical layout container.
     * Everything on this screen is centered vertically and horizontally.
     */
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        ////////////
        /// LOGO ///
        ////////////
        /*
         * App logo.
         */
        Image(
            painter = painterResource(id = R.drawable.bodytrack_logo_ui),
            contentDescription = "BodyTrack Logo",
            modifier = Modifier.size(280.dp)
        )

        /////////////////
        /// APP TITLE ///
        /////////////////
        /*
         * Main app title.
         */
        Text(
            text = "BodyTrack",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        /*
         * Secondary greeting text.
         */
        Text(
            text = "Welcome",
            fontSize = 20.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        //////////////////////
        /// QUOTE CAROUSEL ///
        //////////////////////
        /*
         * Quote container.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {

            /*
             * AnimatedContent automatically animates
             * when targetState (the quote text) changes.
             */
            AnimatedContent(
                targetState = quotes[quoteIndex],
                transitionSpec = {
                    (
                            fadeIn(animationSpec = tween(500)) +
                                    slideInVertically(
                                        animationSpec = tween(500),
                                        initialOffsetY = { it / 4 }
                                    )
                            ) togetherWith
                            (
                                    fadeOut(animationSpec = tween(300)) +
                                            slideOutVertically(
                                                animationSpec = tween(300),
                                                targetOffsetY = { -it / 4 }
                                            )
                                    )
                },
                label = "quote_transition"
            ) { quote ->
                Text(
                    text = quote,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        ////////////////////
        /// START BUTTON ///
        ////////////////////
        Button(
            onClick = onStartClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF2196F3),
                                Color(0xFF21CBF3)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}