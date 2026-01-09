////////////////////////////////////////////////////////////
////////////// BODY TRACK // CLIENT // EXERCISE ////////////
////////////////////////////////////////////////////////////
//////////////////// FILE: ExerciseType ////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.exercise

///////////////////////////////
///// EXERCISE TYPE CLASS /////
///////////////////////////////
/**
 * Represents the types of exercises supported by the Body Track application.
 *
 * This enum serves as a centralized, type-safe registry for all exercises. Using an enum
 * ensures compile-time safety, preventing the use of "magic strings" for exercise identification.
 * It also allows the compiler to enforce exhaustive checks in `when` expressions, ensuring that
 * all exercises are handled consistently throughout the codebase.
 *
 * Each member of this enum corresponds to a specific exercise, such as [SQUAT] or [BICEPS_CURL].
 *
 * @property SQUAT Represents the squat exercise.
 * @property BICEPS_CURL Represents the biceps curl exercise.
 */
enum class ExerciseType {
    SQUAT,
    BICEPS_CURL
}

////////////////////
/// DISPLAY NAME ///
////////////////////
/**
 * Converts an [ExerciseType] enum into a human-readable string suitable for display in the UI.
 *
 * For example, `ExerciseType.BICEPS_CURL` becomes "Biceps Curl".
 *
 * This extension function separates display logic from the core enum definition,
 * allowing for easier localization or changes to UI text without modifying the
 * enum itself.
 *
 * @return A user-friendly [String] representation of the exercise.
 */
fun ExerciseType.displayName(): String =
    when (this) {
        ExerciseType.SQUAT ->
            "Squats"

        ExerciseType.BICEPS_CURL ->
            "Biceps Curl"
    }

///////////////////////////////
/// CAMERA POSITION MAPPING ///
///////////////////////////////
/**
 * Maps an [ExerciseType] to its required camera orientation.
 *
 * This function determines the necessary camera position (e.g., side-on) for accurate
 * pose estimation and analysis of a specific exercise. The value is used to guide the
 * user during the setup phase and may be used for camera validation.
 *
 * For example, a squat requires a side view ([PositionSide.SIDE]) to correctly
 * measure angles at the hip and knee joints.
 *
 * @return The required [PositionSide] for the exercise.
 */
fun ExerciseType.positionSide(): PositionSide =
    when (this) {
        ExerciseType.SQUAT ->
            PositionSide.SIDE

        ExerciseType.BICEPS_CURL ->
            PositionSide.SIDE
    }

/////////////////////////////
/// INITIAL PHASE MAPPING ///
/////////////////////////////
/**
 * Maps an [ExerciseType] to the name of its required initial phase.
 *
 * This function determines the specific starting pose or phase a user must hold
 * before a session can begin. Enforcing a correct initial phase is crucial for
 * accurate and consistent exercise analysis.
 *
 * The returned string must correspond to the phase names defined and expected
 * by the backend pose analysis pipeline.
 *
 * ### Examples:
 * - **SQUAT**: The initial phase is `"TOP"`, meaning the user must be standing fully upright.
 * - **BICEPS_CURL**: The initial phase is `"REST"`, meaning the user's arms must be fully extended downwards.
 *
 * @return A `String` representing the name of the initial phase for this exercise.
 */
fun ExerciseType.initialPhaseName(): String =
    when (this) {
        ExerciseType.SQUAT ->
            "TOP"
        ExerciseType.BICEPS_CURL ->
            "REST"
    }