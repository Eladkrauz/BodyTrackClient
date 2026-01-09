////////////////////////////////////////////////////////////
//////// BODY TRACK // CLIENT // NETWORK // PROTOCOL ///////
////////////////////////////////////////////////////////////
////////////////// FILE: SessionSummary ////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.network.protocol

///////////////
/// IMPORTS ///
///////////////
import org.json.JSONObject

/////////////////////////////
/// SESSION SUMMARY CLASS ///
/////////////////////////////
/**
 * Represents a summary of a completed exercise session.
 *
 * This data class encapsulates all the key metrics and feedback generated after a user
 * finishes a set of exercises. It includes identifiers, performance metrics, detailed breakdowns,
 * and recommendations for improvement.
 *
 * @property sessionId A unique identifier for the exercise session.
 * @property exerciseType The type of exercise performed (e.g., "bicep_curl", "squat").
 * @property sessionDurationSeconds The total duration of the session in seconds.
 * @property numberOfReps The total number of repetitions completed during the session.
 * @property averageRepDurationSeconds The average duration of a single repetition in seconds.
 * @property overallGrade A numerical score from 0.0 to 1.0 representing the overall quality of the session.
 * @property repBreakdown A list of JSON objects, where each object contains detailed metrics for a single repetition.
 * @property aggregatedErrors A JSON object summarizing the errors that occurred across all repetitions.
 * @property recommendations A list of human-readable strings offering advice for improving future performance.
 */
data class SessionSummary(
    val sessionId: String,
    val exerciseType: String,
    val sessionDurationSeconds: Double,
    val numberOfReps: Int,
    val averageRepDurationSeconds: Double,
    val overallGrade: Double,
    val repBreakdown: List<JSONObject>,
    val aggregatedErrors: JSONObject,
    val recommendations: List<String>
)