package com.jc.dobladoryt;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

/**
 * Se encarga de tomar un link de YouTube y devolver la URL directa
 * del stream de audio, usando la libreria open source NewPipeExtractor.
 * Esto evita depender de la API oficial de YouTube (que no permite
 * descargar audio de terceros) y no requiere ninguna API key.
 *
 * IMPORTANTE: esto es para uso personal. Redistribuir o publicar
 * contenido extraido de esta forma puede violar los Terminos de
 * Servicio de YouTube.
 */
public class YoutubeAudioHelper {

    private static boolean initialized = false;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface Callback {
        void onSuccess(String audioUrl, String videoTitle);
        void onError(String mensaje);
    }

    /** Inicializa NewPipe con un Downloader basado en OkHttp (solo una vez). */
    private static void initNewPipe() {
        if (initialized) return;
        NewPipe.init(new Downloader() {
            @Override
            public Response execute(Request request) throws IOException {
                okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                        .url(request.url());

                if (request.dataToSend() != null) {
                    builder.method(request.httpMethod(),
                            RequestBody.create(request.dataToSend()));
                } else {
                    builder.method(request.httpMethod(), null);
                }

                request.headers().forEach((key, values) -> {
                    for (String v : values) builder.addHeader(key, v);
                });

                okhttp3.Response resp = client.newCall(builder.build()).execute();
                String body = resp.body() != null ? resp.body().string() : "";
                return new Response(resp.code(), resp.message(), resp.headers().toMultimap(),
                        body, resp.request().url().toString());
            }
        });
        initialized = true;
    }

    /**
     * Extrae la mejor URL de audio disponible para el link dado.
     * Se ejecuta en un hilo aparte (usar desde un Thread/Executor, nunca en el hilo principal).
     */
    public static void extraerAudio(String youtubeUrl, Callback callback) {
        try {
            initNewPipe();
            StreamExtractor extractor = ServiceList.YouTube
                    .getStreamExtractor(youtubeUrl);
            extractor.fetchPage();

            StreamInfo info = StreamInfo.getInfo(ServiceList.YouTube, youtubeUrl);
            List<AudioStream> audioStreams = info.getAudioStreams();

            if (audioStreams == null || audioStreams.isEmpty()) {
                callback.onError("No se encontraron pistas de audio para este video.");
                return;
            }

            // Elegimos el audio de mejor calidad disponible
            AudioStream mejor = audioStreams.get(0);
            for (AudioStream a : audioStreams) {
                if (a.getAverageBitrate() > mejor.getAverageBitrate()) {
                    mejor = a;
                }
            }

            callback.onSuccess(mejor.getContent(), info.getName());

        } catch (Exception e) {
            callback.onError("Error al extraer audio: " + e.getMessage());
        }
    }
}
