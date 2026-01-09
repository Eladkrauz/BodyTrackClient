////////////////////////////////////////////////////////////
//////// BODY TRACK // CLIENT // NETWORK // PROTOCOL ///////
////////////////////////////////////////////////////////////
/////////////////// FILE: ResponseType /////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.network.protocol

///////////////////////////
/// RESPONSE CODE CLASS ///
///////////////////////////
enum class ResponseType(val value: Int) {
    /**
     * Represents the different types of responses that can be received from the server.
     * Each response type is associated with a unique integer value for serialization/deserialization.
     *
     * @property value The integer representation of the response type.
     */
    PING(1),
    MANAGEMENT(2),
    CALIBRATION(3),
    FEEDBACK(4),
    SUMMARY(5),
    UNKNOWN(6);

    companion object {
        ////////////
        /// FROM ///
        ////////////
        fun from(value: Int):
                /**
                 * Retrieves a [ResponseType] enum constant from its integer value.
                 *
                 * @param value The integer value corresponding to a [ResponseType].
                 * @return The matching [ResponseType] if found, otherwise `null`.
                 */
                ResponseType? = ResponseType.entries.find { it.value == value }
    }
}