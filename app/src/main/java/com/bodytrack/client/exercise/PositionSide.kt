////////////////////////////////////////////////////////////
////////////// BODY TRACK // CLIENT // EXERCISE ////////////
////////////////////////////////////////////////////////////
//////////////////// FILE: PositionSide ////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.exercise

///////////////////////////
/// POSITION SIDE CLASS ///
///////////////////////////
/**
 * Defines the required camera orientation for a specific exercise.
 *
 * This enum is used throughout the client to instruct the user on how to position their camera
 * before starting an exercise and to validate the camera orientation for vision processing.
 *
 * It ensures that camera orientation is restricted to a finite, known set, preventing invalid
 * or ambiguous values and enabling exhaustive `when` expressions in the code.
 *
 * @see FRONT The user is facing the camera directly.
 * @see SIDE The user is positioned sideways relative to the camera.
 */
enum class PositionSide {
    FRONT,
    SIDE
}