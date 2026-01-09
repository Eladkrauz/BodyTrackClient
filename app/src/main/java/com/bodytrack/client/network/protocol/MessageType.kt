////////////////////////////////////////////////////////////
//////// BODY TRACK // CLIENT // NETWORK // PROTOCOL ///////
////////////////////////////////////////////////////////////
//////////////////// FILE: MessageType /////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.network.protocol

//////////////////////////
/// MESSAGE TYPE CLASS ///
//////////////////////////
enum class MessageType(val value: Int) {
    /**
     * Represents the type of a network message.
     *
     * This enum is used to categorize messages sent between the client and server,
     * allowing for proper handling and routing of data based on its purpose.
     *
     * @property value The integer representation of the message type, used for serialization.
     */
    REQUEST(1),
    RESPONSE(2),
    ERROR(3);

    companion object {
        ////////////
        /// FROM ///
        ////////////
        fun from(value: Int):
                /**
                 * Retrieves a [MessageType] enum constant by its integer [value].
                 *
                 * @param value The integer value corresponding to a message type.
                 * @return The matching [MessageType], or `null` if no type with the given value is found.
                 */
                MessageType? = MessageType.entries.find { it.value == value }
    }
}