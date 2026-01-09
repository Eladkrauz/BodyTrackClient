////////////////////////////////////////////////////////////
//////// BODY TRACK // CLIENT // NETWORK // PROTOCOL ///////
////////////////////////////////////////////////////////////
////////////////// FILE: CalibrationCode ///////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.network.protocol

//////////////////////////////
/// CALIBRATION CODE CLASS ///
//////////////////////////////
enum class CalibrationCode(val value: Int) {
    /**
     * Represents the status codes related to user calibration within the system.
     *
     * These codes are used to communicate the state of user visibility and positioning,
     * indicating whether they are currently valid or undergoing a check.
     *
     * @property value The integer representation of the calibration code.
     */
    USER_VISIBILITY_IS_VALID(1),
    USER_VISIBILITY_IS_UNDER_CHECKING(2),
    USER_POSITIONING_IS_VALID(3),
    USER_POSITIONING_IS_UNDER_CHECKING(4);

    companion object {
        ////////////
        /// FROM ///
        ////////////
        fun from(value: Int):
                /**
                 * Retrieves a [CalibrationCode] enum constant from its integer value.
                 *
                 * This function searches through all the defined [CalibrationCode] enum constants
                 * and returns the one that matches the provided integer `value`.
                 *
                 * @param value The integer value corresponding to a [CalibrationCode].
                 * @return The matching [CalibrationCode] if found, otherwise `null`.
                 */
                CalibrationCode? = CalibrationCode.entries.find { it.value == value }
    }
}