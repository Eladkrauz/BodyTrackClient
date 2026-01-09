////////////////////////////////////////////////////////////
//////// BODY TRACK // CLIENT // NETWORK // PROTOCOL ///////
////////////////////////////////////////////////////////////
////////////////// FILE: ManagementCode ////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.network.protocol

/////////////////////////////
/// MANAGEMENT CODE CLASS ///
/////////////////////////////
enum class ManagementCode(val value: Int) {
    /**
     * Represents management codes for communication between the client and server.
     *
     * These codes are used to signal specific states or events related to client session management,
     * server status, and configuration updates.
     *
     * **Important:** The integer values of these enums must exactly match the corresponding
     * `ManagementCode` enum on the server-side to ensure proper protocol interpretation.
     *
     * @property value The integer representation of the management code.
     */
    CLIENT_REGISTERED_SUCCESSFULLY(1),
    CLIENT_SESSION_IS_REGISTERED(2),
    CLIENT_SESSION_IS_ACTIVE(3),
    CLIENT_SESSION_IS_PAUSED(4),
    CLIENT_SESSION_IS_RESUMED(5),
    CLIENT_SESSION_IS_ENDED(6),
    CLIENT_SESSION_IS_UNREGISTERED(7),
    CLIENT_SESSION_IS_NOT_IN_SYSTEM(8),
    SERVER_IS_BEING_SHUTDOWN(9),
    CONFIGURATION_UPDATED_SUCCESSFULLY(10),
    SESSION_IS_STARTING(11);

    companion object {
        ////////////
        /// FROM ///
        ////////////
        fun from(value: Int):
                /**
                 * Returns the [ManagementCode] corresponding to the given integer [value].
                 *
                 * @param value The integer value of the management code.
                 * @return The matching [ManagementCode] enum constant, or `null` if no match is found.
                 */
                ManagementCode? = ManagementCode.entries.find { it.value == value }
    }
}
