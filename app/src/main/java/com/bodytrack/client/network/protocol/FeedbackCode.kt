////////////////////////////////////////////////////////////
//////// BODY TRACK // CLIENT // NETWORK // PROTOCOL ///////
////////////////////////////////////////////////////////////
//////////////////// FILE: FeedbackCode ////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.network.protocol

///////////////////////////
/// FEEDBACK CODE CLASS ///
///////////////////////////
enum class FeedbackCode(val value: Int) {
    /**
     * Represents feedback codes received from the server during a workout session.
     *
     * This enum class defines a set of standardized codes that correspond to specific feedback messages
     * for the user. These codes cover system states, general pose quality errors, and specific errors
     * for various exercises like squats and bicep curls.
     *
     * Each code has an integer [value] that MUST EXACTLY MATCH the corresponding code on the server-side.
     * The integrity of this mapping is critical for correct audio feedback, on-screen instructions, and
     * repetition counting logic.
     *
     * **IMPORTANT:** To maintain compatibility with the server, do not reorder or remove existing entries.
     * New feedback codes should only be appended to the end of the list.
     *
     * @property value The unique integer identifier for the feedback code.
     *
     * @see [com.bodytrack.client.network.protocol] for related protocol classes.
     */
    // System states.
    VALID(1),
    SILENT(2),

    // Pose quality errors.
    NO_PERSON(3),
    PARTIAL_BODY(4),
    TOO_FAR(5),
    UNSTABLE(6),

    // Squats errors.
    SQUAT_TOP_KNEE_TOO_STRAIGHT(7),
    SQUAT_TOP_KNEE_TOO_BENT(8),
    SQUAT_TOP_HIP_TOO_STRAIGHT(9),
    SQUAT_TOP_HIP_TOO_BENT(10),
    SQUAT_TOP_TRUNK_TOO_FORWARD(11),
    SQUAT_TOP_TRUNK_TOO_BACKWARD(12),
    SQUAT_TOP_HIP_LINE_UNBALANCED(13),

    SQUAT_DOWN_TRUNK_TOO_FORWARD(14),
    SQUAT_DOWN_TRUNK_TOO_BACKWARD(15),
    SQUAT_DOWN_HIP_LINE_UNBALANCED(16),

    SQUAT_HOLD_KNEE_TOO_STRAIGHT(17),
    SQUAT_HOLD_KNEE_TOO_BENT(18),
    SQUAT_HOLD_HIP_TOO_HIGH(19),
    SQUAT_HOLD_HIP_TOO_DEEP(20),
    SQUAT_HOLD_TRUNK_TOO_FORWARD(21),
    SQUAT_HOLD_TRUNK_TOO_BACKWARD(22),
    SQUAT_HOLD_HIP_LINE_UNBALANCED(23),

    SQUAT_UP_TRUNK_TOO_FORWARD(24),
    SQUAT_UP_TRUNK_TOO_BACKWARD(25),
    SQUAT_UP_HIP_LINE_UNBALANCED(26),

    // Bicep curls errors.
    CURL_REST_ELBOW_TOO_BENT(27),
    CURL_REST_ELBOW_TOO_STRAIGHT(28),
    CURL_REST_SHOULDER_TOO_FORWARD(29),
    CURL_REST_SHOULDER_TOO_BACKWARD(30),

    CURL_LIFTING_ELBOW_TOO_STRAIGHT(31),
    CURL_LIFTING_ELBOW_TOO_BENT(32),
    CURL_LIFTING_SHOULDER_TOO_FORWARD(33),
    CURL_LIFTING_SHOULDER_TOO_BACKWARD(34),

    CURL_HOLD_ELBOW_TOO_OPEN(35),
    CURL_HOLD_ELBOW_TOO_CLOSED(36),
    CURL_HOLD_WRIST_TOO_FLEXED(37),
    CURL_HOLD_WRIST_TOO_EXTENDED(38),

    CURL_LOWERING_ELBOW_TOO_STRAIGHT(39),
    CURL_LOWERING_ELBOW_TOO_BENT(40),
    CURL_LOWERING_SHOULDER_TOO_FORWARD(41),
    CURL_LOWERING_SHOULDER_TOO_BACKWARD(42);

    companion object {
        ////////////
        /// FROM ///
        ////////////
        fun from(value: Int):
                /**
                 * Creates a [FeedbackCode] from its integer representation.
                 *
                 * This function searches through all the enum constants to find the one
                 * matching the provided [value]. This is useful for deserializing
                 * the feedback code received from the server.
                 *
                 * @param value The integer value of the feedback code.
                 * @return The corresponding [FeedbackCode] enum constant, or `null` if no
                 *   matching code is found.
                 */
                FeedbackCode? = FeedbackCode.entries.find { it.value == value }
    }
}