////////////////////////////////////////////////////////////
//////// BODY TRACK // CLIENT // NETWORK // PROTOCOL ///////
////////////////////////////////////////////////////////////
///////////////////// FILE: ErrorCode //////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.network.protocol

////////////////////////
/// ERROR CODE CLASS ///
////////////////////////
/**
 * Represents a comprehensive list of error codes used throughout the application.
 * Each error code is associated with a unique integer value, facilitating error
 * identification and handling across different modules. The errors are categorized
 * by their functional area.
 *
 * @property value The unique integer identifier for the error code.
 */
enum class ErrorCode(val value: Int) {

    // Configuration and management.
    CANT_CONFIGURE_LOG(1),
    CONFIGURATION_FILE_DOES_NOT_EXIST(2),
    JSON_FILE_DECODE_ERROR(3),
    JSON_FILE_UNICODE_ERROR(4),
    CONFIG_PARAM_DOES_NOT_EXIST(5),
    CRITICAL_CONFIG_PARAM_DOES_NOT_EXIST(6),
    CONFIGURATION_KEY_IS_INVALID(7),
    INVALID_JSON_PAYLOAD_IN_REQUEST(8),
    MISSING_EXERCISE_TYPE_IN_REQUEST(9),
    MISSING_SESSION_ID_IN_REQUEST(10),
    MISSING_FRAME_DATA_IN_REQUEST(11),
    CLIENT_IP_IS_INVALID(12),
    CLIENT_AGENT_IS_INVALID(13),
    JSON_CONFIG_FILE_ERROR(14),
    INTERNAL_SERVER_ERROR(15),

    // Flask and database management.
    CANT_ADD_URL_RULE_TO_FLASK_SERVER(16),
    UNRECOGNIZED_ICD_ERROR_TYPE(17),
    UNRECOGNIZED_ICD_RESPONSE_TYPE(18),
    DATABASE_CONNECTION_FAILED(19),
    DATABASE_SCHEMA_CREATION_FAILED(20),
    USER_ALREADY_EXISTS(21),
    DATABASE_INSERT_FAILED(22),
    DATABASE_QUERY_FAILED(23),
    DATABASE_UPDATE_FAILED(24),
    DATABASE_DELETE_FAILED(25),
    USER_INVALID_CREDENTIALS(26),
    USER_IS_ALREADY_LOGGED_IN(27),
    USER_IS_NOT_LOGGED_IN(28),
    USER_NOT_FOUND(29),
    FRAME_DECODING_FAILED(30),
    TERMINATION_INCORRECT_PASSWORD(31),

    // Session Manager.
    EXERCISE_TYPE_DOES_NOT_EXIST(32),
    ERROR_GENERATING_SESSION_ID(33),
    MAX_CLIENT_REACHED(34),
    INVALID_SESSION_ID(35),
    CLIENT_IS_NOT_REGISTERED(36),
    CLIENT_IS_ALREADY_REGISTERED(37),
    CLIENT_IS_NOT_ACTIVE(38),
    CLIENT_IS_ALREADY_ACTIVE(39),
    CLIENT_IS_NOT_PAUSED(40),
    CLIENT_IS_ALREADY_PAUSED(41),
    CLIENT_IS_NOT_ENDED(42),
    CLIENT_IS_ALREADY_ENDED(43),
    CLIENT_IS_ONLY_REGISTERED(44),
    SEARCH_TYPE_IS_NOT_SUPPORTED(45),
    SESSION_STATUS_IS_NOT_RECOGNIZED(46),
    FRAME_INITIAL_VALIDATION_FAILED(47),
    INVALID_EXTENDED_EVALUATION_PARAM(48),
    TRYING_TO_ANALYZE_FRAME_WHEN_DONE(49),
    TRYING_TO_ANALYZE_FRAME_WHEN_FAILED(50),
    ERROR_CREATING_SESSION_DATA(51),
    SESSION_SHOULD_ABORT(52),
    CLIENT_NOT_IN_SYSTEM(53),

    // PoseAnalyzer.
    ERROR_INITIALIZING_POSE(54),
    FRAME_PREPROCESSING_ERROR(55),
    FRAME_VALIDATION_ERROR(56),
    FRAME_ANALYSIS_ERROR(57),

    // JointAnalyzer.
    VECTOR_VALIDATION_FAILED(58),
    ANGLE_VALIDATION_FAILED(59),
    ANGLE_CALCULATION_FAILED(60),
    JOINT_CALCULATION_ERROR(61),
    DIMENSION_OF_ANGLE_IS_INVALID(62),
    TOO_MANY_INVALID_ANGLES(63),
    CANT_CALCULATE_JOINTS_OF_UNSTALBE_FRAME(64),
    ANGLES_DICTIONARY_IS_EMPTY(65),

    // HistoryManager.
    HISTORY_MANAGER_INIT_ERROR(66),
    EXERCISE_START_TIME_ALREADY_SET(67),
    EXERCISE_END_TIME_ALREADY_SET(68),
    HISTORY_MANAGER_INTERNAL_ERROR(69),
    TRIED_TO_ADD_ERROR_TO_NONE_REP(70),
    TRIED_TO_END_A_NONE_REP(71),
    TRIED_TO_START_REP_WHILE_HAVE_ONE(72),
    ERROR_WITH_HANDLING_FRAMES_LIST(73),
    CANT_FIND_FRAME_IN_FRAMES_WINDOW(74),
    LAST_VALID_FRAME_IS_NONE(75),
    ERROR_WITH_HANDLING_BAD_FRAMES_LIST(76),

    // PoseQualityManager.
    FAILED_TO_INITIALIZE_QUALITY_MANAGER(77),
    QUALITY_CHECKING_ERROR(78),
    NO_PERSON_DETECTED_IN_FRAME(79),
    PARTIAL_BODY_IN_FRAME(80),
    TOO_FAR_IN_FRAME(81),
    UNSTABLE_IN_FRAME(82),
    LAST_VALID_FRAME_MISSING(83),

    // ErrorDetector.
    ERROR_DETECTOR_INVALID_ANGLE(84),
    ERROR_DETECTOR_MISSING_THRESHOLD(85),
    ERROR_DETECTOR_MAPPING_NOT_FOUND(86),
    ERROR_DETECTOR_UNSUPPORTED_PHASE(87),
    ERROR_DETECTOR_INIT_ERROR(88),
    ERROR_DETECTOR_CONFIG_ERROR(89),

    // PhaseDetector.
    PHASE_THRESHOLDS_CONFIG_FILE_ERROR(90),
    NO_VALID_FRAME_DATA_IN_SESSION(91),
    PHASE_UNDETERMINED_IN_FRAME(92),
    TRIED_TO_DETECT_FRAME_FOR_UNSTABLE_STATE(93),
    PHASE_IS_NONE_IN_FRAME(94),

    // FeedbackFormatter.
    FEEDBACK_FORMATTER_INIT_ERROR(95),
    FEEDBACK_CONSTRUCTION_ERROR(96),
    POSE_QUALITY_FEEDBACK_SELECTION_ERROR(97),
    BIOMECHANICAL_FEEDBACK_SELECTION_ERROR(98),
    FEEDBACK_CONFIG_RETRIEVAL_ERROR(99),

    // SessionSummaryManager.
    SUMMARY_MANAGER_INIT_ERROR(100),
    SUMMARY_MANAGER_CREATE_ERROR(101),
    SUMMARY_MANAGER_INTERNAL_ERROR(102),

    // PositionSideDetector.
    FAILED_TO_INITIALIZE_SIDE_DETECTOR(103),
    POSITION_SIDE_DETECTION_ERROR(104),
    WRONG_EXERCISE_POSITION(105),
    POSITION_SIDE_DOES_NOT_EXIST(106);

    companion object {
        ////////////
        /// FROM ///
        ////////////
        fun from(value: Int):
                /**
                 * Retrieves an [ErrorCode] instance that corresponds to the given integer value.
                 *
                 * This function searches through all the defined `ErrorCode` enum constants
                 * and returns the one whose `value` property matches the input integer.
                 *
                 * @param value The integer value of the error code to find.
                 * @return The matching [ErrorCode] if found, otherwise `null`.
                 */
                ErrorCode? = ErrorCode.entries.find { it.value == value }
    }
}