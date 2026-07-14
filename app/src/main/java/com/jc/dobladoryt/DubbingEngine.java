package com.jc.dobladoryt;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.common.model.DownloadConditions;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Orquesta las 3 etapas, todas con herramientas gratuitas integradas
 * en Android / Google Play Services:
 *
 * 1) SpeechRecognizer (nativo Android)  -> escucha ingles por el microfono
 *    Nota: escucha lo que capte el microfono. Para doblar un video ya
 *    reproducido, hay que reproducirlo por el altavoz para que el mic lo oiga.
 * 2) ML Kit Translate (Google, offline tras descargar el modelo) -> traduce
 * 3) TextToSpeech (nativo Android) -> habla el resultado en espanol
 */
public class DubbingEngine {

    public interface Listener {
        void onTranscripcionParcial(String textoIngles);
        void onTraduccionLista(String textoEspanol);
        void onEstado(String mensaje);
        void onError(String mensaje);
    }

    private final Context context;
    private final Listener listener;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Translator translator;
    private boolean escuchando = false;

    public DubbingEngine(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        inicializarTraductor();
        inicializarTts();
    }

    private void inicializarTraductor() {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.SPANISH)
                .build();
        translator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        listener.onEstado("Descargando modelo de traduccion (solo la primera vez)...");
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(v -> listener.onEstado("Modelo de traduccion listo."))
                .addOnFailureListener(e -> listener.onError("No se pudo descargar el modelo de traduccion: " + e.getMessage()));
    }

    private void inicializarTts() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("spa", "MEX"));
            } else {
                listener.onError("No se pudo iniciar el sintetizador de voz.");
            }
        });
    }

    /** Inicia la escucha continua por microfono en ingles. */
    public void iniciarEscucha() {
        if (escuchando) return;
        escuchando = true;

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                listener.onEstado("Escuchando...");
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override public void onError(int error) {
                listener.onEstado("Reintentando escucha...");
                if (escuchando) reiniciarEscucha();
            }

            @Override public void onResults(Bundle results) {
                ArrayList<String> frases = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (frases != null && !frases.isEmpty()) {
                    String textoIngles = frases.get(0);
                    listener.onTranscripcionParcial(textoIngles);
                    traducirYHablar(textoIngles);
                }
                if (escuchando) reiniciarEscucha();
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        lanzarEscucha();
    }

    private void lanzarEscucha() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizer.startListening(intent);
    }

    private void reiniciarEscucha() {
        if (speechRecognizer != null && escuchando) {
            speechRecognizer.cancel();
            lanzarEscucha();
        }
    }

    private void traducirYHablar(String textoIngles) {
        translator.translate(textoIngles)
                .addOnSuccessListener(textoEspanol -> {
                    listener.onTraduccionLista(textoEspanol);
                    hablar(textoEspanol);
                })
                .addOnFailureListener(e -> listener.onError("Error al traducir: " + e.getMessage()));
    }

    private void hablar(String textoEspanol) {
        if (textToSpeech != null) {
            textToSpeech.speak(textoEspanol, TextToSpeech.QUEUE_ADD, null, null);
        }
    }

    /** Detiene todo (escucha, traductor, voz). */
    public void detener() {
        escuchando = false;
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    public void liberar() {
        detener();
        if (translator != null) translator.close();
        if (textToSpeech != null) textToSpeech.shutdown();
    }
}
