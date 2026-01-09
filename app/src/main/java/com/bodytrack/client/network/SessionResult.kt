////////////////////////////////////////////////////////////
////////////// BODY TRACK // CLIENT // NETWORK /////////////
////////////////////////////////////////////////////////////
/////////////////// FILE: SessionResult ////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.network

///////////////
/// IMPORTS ///
///////////////
import com.bodytrack.client.network.protocol.CalibrationCode
import com.bodytrack.client.network.protocol.ErrorCode
import com.bodytrack.client.network.protocol.FeedbackCode
import com.bodytrack.client.network.protocol.ManagementCode
import com.bodytrack.client.network.protocol.SessionSummary
import org.json.JSONObject
import java.io.IOException

////////////////////////////
/// SESSION RESULT CLASS ///
////////////////////////////
/**
 * Represents the various possible outcomes of a network session with the server.
 * This is a sealed class, which means it can only be one of the defined subtypes,
 * providing a type-safe way to handle all possible results.
 *
 * The results are categorized into three main types:
 * - [Success]: Represents a successful communication where the server responded as expected.
 *   This is further divided into specific success types like Calibration, Management, etc.
 * - [Error]: Represents a situation where the server responded with a specific error code
 *   and a descriptive message.
 * - [NetworkFailure]: Represents a failure at the network level, such as a connection timeout
 *   or an I/O problem, encapsulated by an [IOException].
 */
sealed class SessionResult {
    ///////////////
    /// SUCCESS ///
    ///////////////
    /**
     * Represents a successful result from a session interaction.
     *
     * This is a sealed class that encapsulates various types of successful responses
     * that can be received from the server during a session. Each subclass corresponds
     * to a specific category of successful operation.
     */
    sealed class Success : SessionResult() {
        /**
         * Represents a successful calibration result from a session.
         *
         * This result is typically received during the calibration phase of a session,
         * providing updates or confirmations about the process.
         *
         * @property code The specific [CalibrationCode] indicating the type of calibration event.
         * @property description A human-readable string describing the calibration event.
         * @property extraInfo An optional [JSONObject] containing additional data related to the calibration event, such as specific calibration values or state information.
         */
        data class Calibration(
            val code: CalibrationCode,
            val description: String,
            val extraInfo: JSONObject?
        ) : Success()

        /**
         * Represents a successful management event from the session.
         *
         * This result is returned for session management actions, such as starting, stopping,
         * or pausing a session. It provides a code to identify the specific event and a
         * human-readable description.
         *
         * @property code The specific management event code, defined in [ManagementCode].
         * @property description A human-readable string describing the management event.
         * @property extraInfo An optional [JSONObject] containing additional data related to the event.
         */
        data class Management(
            val code: ManagementCode,
            val description: String,
            val extraInfo: JSONObject?
        ) : Success()

        /**
         * Represents a successful feedback result from the session.
         *
         * This is typically received in response to data sent to the server, providing
         * real-time feedback or status updates.
         *
         * @property code The specific feedback code indicating the type of feedback.
         * @property description A human-readable description of the feedback.
         * @property extraInfo An optional JSON object containing additional data related to the feedback.
         */
        data class Feedback(
            val code: FeedbackCode,
            val description: String,
            val extraInfo: JSONObject?
        ) : Success()

        /**
         * Represents a successful ping response from the server.
         * This is typically used to confirm that the connection is alive and the server is responsive.
         *
         * @property description A human-readable message confirming the successful ping, e.g., "pong".
         */
        data class Ping(
            val description: String
        ) : Success()

        /**
         * Represents a successful session result containing a summary of the session.
         * This is typically received at the end of a session and contains aggregated data.
         *
         * @property summary The [SessionSummary] object containing detailed information about the completed session.
         */
        data class Summary(
            val summary: SessionSummary
        ) : Success()
    }

    /////////////
    /// ERROR ///
    /////////////
    /**
     * Represents an error result from the session.
     * This is used for server-side errors that are gracefully handled and reported back to the client.
     *
     * @property code The specific [ErrorCode] identifying the type of error.
     * @property description A human-readable description of the error.
     */
    data class Error(
        val code: ErrorCode,
        val description: String
    ) : SessionResult()

    ///////////////////////
    /// NETWORK FAILURE ///
    ///////////////////////
    /**
     * Represents a network-level failure that prevented communication with the server.
     * This is distinct from an [Error] result, which indicates a server-side error
     * after a successful connection.
     *
     * @property exception The [IOException] that caused the network failure.
     */
    data class NetworkFailure(
        val exception: IOException
    ) : SessionResult()
}