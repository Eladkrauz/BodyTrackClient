////////////////////////////////////////////////////////////
////////////// BODY TRACK // CLIENT // SUMMARY /////////////
////////////////////////////////////////////////////////////
///////////////// FILE: SessionSummaryScreen ///////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.summary

///////////////
/// IMPORTS ///
///////////////
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bodytrack.client.network.SessionApi
import com.bodytrack.client.network.SessionResult
import com.bodytrack.client.network.protocol.SessionSummary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/////////////////////////////////////////
/// SESSION SUMMARY SCREEN COMPOSABLE ///
/////////////////////////////////////////
/**
 * A composable that displays a summary of a completed workout session.
 *
 * This screen fetches the session summary data using the provided [sessionId].
 * It manages loading and error states. While the data is being fetched, it shows
 * a loading indicator for a minimum duration to prevent flickering. If an error
 * occurs during the fetch, it displays an error message. Upon successful data
 * retrieval, it presents the session summary, including overall grade, rep breakdown,
 * errors, and recommendations.
 *
 * @param sessionId The unique identifier for the session to fetch the summary for.
 * @param onDone A callback function to be invoked when the user clicks the "Done"
 *               or "Back to Home" button, typically to navigate away from this screen.
 */
@Composable
fun SessionSummaryScreen(
    sessionId: String,
    onDone: () -> Unit
) {
    var summary by remember { mutableStateOf<SessionSummary?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var minDelayPassed by remember { mutableStateOf(false) }
    var responseArrived by remember { mutableStateOf(false) }

    // Minimum loader time.
    LaunchedEffect(Unit) {
        delay(2_000)
        minDelayPassed = true
    }

    // Fetch summary.
    LaunchedEffect(sessionId) {
        SessionApi.sessionSummary(sessionId) { result ->
            when (result) {
                is SessionResult.Success.Summary -> {
                    summary = result.summary
                    responseArrived = true
                }
                is SessionResult.Error -> {
                    errorMessage = result.description
                    responseArrived = true
                }
                is SessionResult.NetworkFailure -> {
                    errorMessage = "Network error while loading session summary."
                    responseArrived = true
                }
                else -> {}
            }
        }
    }
    val showLoader = !(minDelayPassed && responseArrived)

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            showLoader -> SummaryLoading()
            errorMessage != null -> SummaryError(errorMessage!!, onDone)
            summary != null -> SummaryContent(summary!!, onDone)
        }
    }
}

///////////////////////
/// SUMMARY LOADING ///
///////////////////////
/**
 * A composable that displays a loading indicator and a text message.
 * This is shown while the session summary is being fetched and processed.
 * It ensures a minimum display time to prevent jarring flashes of content.
 */
@Composable
private fun SummaryLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Combining Session Summary", fontWeight = FontWeight.Medium)
    }
}

/////////////////////
/// SUMMARY ERROR ///
/////////////////////
/**
 * A composable function that displays an error message and a button to return to the home screen.
 * This is shown when there's an issue fetching the session summary, such as a network error
 * or an error from the server.
 *
 * @param message The error message to be displayed to the user.
 * @param onDone A callback function that is invoked when the "Back to Home" button is clicked,
 *               typically used for navigation.
 */
@Composable
private fun SummaryError(message: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = Color.Red)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDone) {
            Text("Back to Home")
        }
    }
}

///////////////////////
/// SUMMARY CONTENT ///
///////////////////////
/**
 * The main content of the session summary screen.
 * This composable arranges various sections of the summary in a vertical list.
 * It conditionally displays sections based on whether data for them exists in the [summary] object.
 *
 * @param summary The [SessionSummary] data object containing all the information to be displayed.
 * @param onDone A callback function to be invoked when the "Back to Home" button is clicked.
 */
@Composable
private fun SummaryContent(
    summary: SessionSummary,
    onDone: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { HeaderSection(summary) }
        item { GradeSection(summary.overallGrade) }

        if (summary.repBreakdown.isNotEmpty()) {
            item { RepTableSection(summary.repBreakdown) }
        }

        if (summary.aggregatedErrors.length() > 0) {
            item { AggregatedErrorsSection(summary.aggregatedErrors) }
        }

        if (summary.recommendations.isNotEmpty()) {
            item { RecommendationsSection(summary.recommendations) }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDone
            ) {
                Text("Back to Home")
            }
        }
    }
}

//////////////////////
/// HEADER SECTION ///
//////////////////////
/**
 * A Composable that displays the header information of a session summary.
 * This includes the exercise type, total duration, number of repetitions,
 * and the average duration of each repetition.
 *
 * @param summary The [SessionSummary] object containing the data to display.
 */
@Composable
private fun HeaderSection(summary: SessionSummary) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                summary.exerciseType.replace("_", " ")
                    .lowercase()
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text("Duration: ${summary.sessionDurationSeconds.roundToInt()} seconds")
            Text("Repetitions: ${summary.numberOfReps}")
            Text("Average rep duration: ${"%.2f".format(summary.averageRepDurationSeconds)} seconds")
        }
    }
}

/////////////////////
/// GRADE SECTION ///
/////////////////////
/**
 * A Composable that displays the overall session grade in a card.
 * It features a circular progress indicator (pie chart style) representing the grade,
 * with the numerical score overlaid in the center. The color of the arc changes based on the grade's value
 * to provide a quick visual cue of performance (Red for poor, Orange for fair, Yellow for good, Green for excellent).
 *
 * @param grade The overall grade of the session, expected to be a value between 0 and 100.
 */
@Composable
private fun GradeSection(grade: Double) {
    val color = when (grade.roundToInt()) {
        in 0..55 -> Color.Red
        in 56..70 -> Color(0xFFFF9800)
        in 71..84 -> Color.Yellow
        else -> Color.Green
    }

    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Overall Grade", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = (grade.toFloat() / 100f) * 360f,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                }
                Text(
                    "${"%.2f".format(grade)} / 100",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

/////////////////////////
/// REP TABLE SECTION ///
/////////////////////////
/**
 * A Composable that displays a table of repetition breakdown for a session.
 * It shows a header row and then iterates through each repetition, displaying its details
 * in a `RepTableRow`.
 *
 * @param reps A list of JSONObjects, where each object represents a single repetition
 *             and contains its details like duration and correctness.
 */
@Composable
private fun RepTableSection(reps: List<org.json.JSONObject>) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Repetition Breakdown", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            // Header row.
            Row(
                Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Rep", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Duration", Modifier.weight(2f), fontWeight = FontWeight.Bold)
                Text("Result", Modifier.weight(2f), fontWeight = FontWeight.Bold)
            }

            reps.forEachIndexed { index, rep ->
                RepTableRow(index + 1, rep)
            }
        }
    }
}

/////////////////////
/// REP TABLE ROW ///
/////////////////////
/**
 * A composable that displays a single row in the repetition breakdown table.
 *
 * This row shows the repetition's index, duration, and whether it was performed correctly.
 * If the repetition was incorrect and has associated errors, the row is clickable to expand
 * and show a list of specific errors.
 *
 * @param index The 1-based index of the repetition in the session.
 * @param rep A [org.json.JSONObject] containing the data for this repetition, including
 *            its duration, correctness status, and a list of errors if any.
 */
@Composable
private fun RepTableRow(index: Int, rep: org.json.JSONObject) {
    var expanded by remember { mutableStateOf(false) }
    val isCorrect = rep.optBoolean("is_correct", true)
    val errors = rep.optJSONArray("errors")

    Column(
        Modifier.fillMaxWidth()
            .clickable(enabled = !isCorrect && errors != null) { expanded = !expanded }
            .border(0.5.dp, Color.LightGray)
            .padding(8.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("$index", Modifier.weight(1f))
            Text("${rep.optDouble("duration")} s", Modifier.weight(2f))
            Text(
                if (isCorrect) "Correct" else "Incorrect",
                Modifier.weight(2f),
                color = if (isCorrect) Color.Green else Color.Red
            )
        }

        if (expanded && errors != null) {
            Spacer(Modifier.height(6.dp))
            for (i in 0 until errors.length()) {
                Text(
                    "• ${errors.getString(i).replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}",
                    color = Color.Red
                )
            }
        }
    }
}

/////////////////////////////////
/// AGGREGATED ERRORS SECTION ///
/////////////////////////////////
/**
 * A Composable that displays a card containing a summary of aggregated errors from the session.
 * It lists each type of error and how many times it occurred.
 *
 * @param errors A JSONObject where keys are error types (e.g., "knee_too_far_forward")
 *               and values are the count of each error's occurrence.
 */
@Composable
private fun AggregatedErrorsSection(errors: org.json.JSONObject) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Aggregated Errors", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            errors.keys().forEach { key ->
                Text(
                    "${key.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}: ${errors.optInt(key)}"
                )
            }
        }
    }
}

////////////////////////////////
/// RECOMMENDATIONS SECTIONS ///
////////////////////////////////
/**
 * A Composable that displays a list of recommendations for the user based on their
 * session performance. The recommendations are presented in a Card as a bulleted list.
 *
 * @param recs A list of strings, where each string is a single recommendation to be displayed.
 */
@Composable
private fun RecommendationsSection(recs: List<String>) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Recommendations", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            recs.forEach {
                Text("• $it")
            }
        }
    }
}