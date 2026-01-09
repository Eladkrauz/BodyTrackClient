////////////////////////////////////////////////////////////
////////////// BODY TRACK // CLIENT // SESSION /////////////
////////////////////////////////////////////////////////////
///////////////////// FILE: TtsManager /////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.session

///////////////
/// IMPORTS ///
///////////////
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/////////////////////////
/// TTS MANAGER CLASS ///
/////////////////////////
/**
 * Manages Text-to-Speech (TTS) functionality for the application.
 *
 * This class encapsulates the Android `TextToSpeech` engine, providing a simplified
 * interface for speaking text. It handles the asynchronous initialization of the TTS engine
 * and manages a queue for speech requests. Callbacks can be provided to execute code
 * once the speech synthesis is complete.
 *
 * It is crucial to call [shutdown] when this manager is no longer needed (e.g., in `onDestroy`)
 * to release the underlying TTS engine resources.
 *
 * @param context The application context used to initialize the `TextToSpeech` engine.
 */
class TtsManager(context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isReady = AtomicBoolean(false)
    private var onDoneCallback: (() -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(1.0f)
            tts.setPitch(1.0f)
            isReady.set(true)
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    onDoneCallback?.invoke()
                    onDoneCallback = null
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    onDoneCallback?.invoke()
                    onDoneCallback = null
                }
            }
        })
    }

    /////////////
    /// SPEAK ///
    /////////////
    /**
     * Speaks the given text using the Text-to-Speech engine.
     * The request is added to the synthesis queue and will be spoken after any preceding requests.
     *
     * This function will do nothing if the TTS engine is not yet initialized.
     *
     * @param text The text to be spoken.
     * @param onCompleted An optional callback that is invoked on the main thread when the utterance has
     *                    finished being spoken. This callback is also invoked if an error occurs.
     */
    fun speak(text: String, onCompleted: (() -> Unit)? = null) {
        if (!isReady.get()) return
        onDoneCallback = onCompleted
        tts.speak(
            text,
            TextToSpeech.QUEUE_ADD,
            null,
            UUID.randomUUID().toString()
        )
    }

    ////////////////
    /// SHUTDOWN ///
    ////////////////
    /**
     * Stops any ongoing speech and releases the resources used by the TextToSpeech engine.
     * This method should be called when the TTS service is no longer needed, for example,
     * in the onDestroy() lifecycle method of an Activity or Fragment, to prevent memory leaks.
     */
    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}