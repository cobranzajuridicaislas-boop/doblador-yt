package com.jc.dobladoryt;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements DubbingEngine.Listener {

    private EditText editUrl;
    private TextView txtOriginal, txtTraducido, txtEstado;
    private Button btnExtraer, btnDoblar, btnDetener;

    private DubbingEngine dubbingEngine;
    private MediaPlayer mediaPlayer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final int REQ_MICROFONO = 100;

    private void instalarCapturadorDeErrores() {
        final Thread.UncaughtExceptionHandler manejadorPrevio = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                File archivo = new File(getFilesDir(), "ultimo_error.txt");
                try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
                    throwable.printStackTrace(pw);
                }
            } catch (Exception ignored) {
            }
            if (manejadorPrevio != null) {
                manejadorPrevio.uncaughtException(thread, throwable);
            }
        });
    }

    private void mostrarUltimoErrorSiExiste() {
        File archivo = new File(getFilesDir(), "ultimo_error.txt");
        if (!archivo.exists()) return;
        try {
            StringBuilder contenido = new StringBuilder();
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(archivo));
            String linea;
            while ((linea = br.readLine()) != null) {
                contenido.append(linea).append("\n");
            }
            br.close();
            new AlertDialog.Builder(this)
                    .setTitle("La app se cerro la ultima vez. Este fue el error:")
                    .setMessage(contenido.toString())
                    .setPositiveButton("OK", (d, w) -> archivo.delete())
                    .show();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instalarCapturadorDeErrores();
        setContentView(R.layout.activity_main);

        editUrl = findViewById(R.id.editUrl);
        txtOriginal = findViewById(R.id.txtOriginal);
        txtTraducido = findViewById(R.id.txtTraducido);
        txtEstado = findViewById(R.id.txtEstado);
        btnExtraer = findViewById(R.id.btnExtraer);
        btnDoblar = findViewById(R.id.btnDoblar);
        btnDetener = findViewById(R.id.btnDetener);

        dubbingEngine = new DubbingEngine(this, this);

        btnExtraer.setOnClickListener(v -> extraerYReproducir());
        btnDoblar.setOnClickListener(v -> iniciarDoblaje());
        btnDetener.setOnClickListener(v -> detenerTodo());

        pedirPermisoMicrofono();
        mostrarUltimoErrorSiExiste();
    }

    private void pedirPermisoMicrofono() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MICROFONO);
        }
    }

    private void extraerYReproducir() {
        try {
            String url = editUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Pega un link de YouTube primero", Toast.LENGTH_SHORT).show();
                return;
            }
            onEstado("Extrayendo audio del video...");

            executor.execute(() -> {
                try {
                    YoutubeAudioHelper.extraerAudio(url, new YoutubeAudioHelper.Callback() {
                        @Override
                        public void onSuccess(String audioUrl, String videoTitle) {
                            runOnUiThread(() -> {
                                onEstado("Reproduciendo: " + videoTitle);
                                reproducirAudio(audioUrl);
                            });
                        }

                        @Override
                        public void onError(String mensaje) {
                            runOnUiThread(() -> MainActivity.this.onError(mensaje));
                        }
                    });
                } catch (Throwable t) {
                    runOnUiThread(() -> onError("Error inesperado: " + t.getClass().getSimpleName() + ": " + t.getMessage()));
                }
            });
        } catch (Throwable t) {
            onError("Error inesperado: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private void reproducirAudio(String audioUrl) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            onError("Error al reproducir: " + e.getMessage());
        }
    }

    private void iniciarDoblaje() {
        onEstado("Iniciando doblaje. Asegurate de que el audio se este reproduciendo por el altavoz.");
        dubbingEngine.iniciarEscucha();
    }

    private void detenerTodo() {
        dubbingEngine.detener();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        onEstado("Detenido.");
    }

    // --- callbacks de DubbingEngine.Listener ---

    @Override
    public void onTranscripcionParcial(String textoIngles) {
        runOnUiThread(() -> txtOriginal.setText(textoIngles));
    }

    @Override
    public void onTraduccionLista(String textoEspanol) {
        runOnUiThread(() -> txtTraducido.setText(textoEspanol));
    }

    @Override
    public void onEstado(String mensaje) {
        runOnUiThread(() -> txtEstado.setText("Estado: " + mensaje));
    }

    @Override
    public void onError(String mensaje) {
        runOnUiThread(() -> {
            txtEstado.setText("Error: " + mensaje);
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dubbingEngine.liberar();
        if (mediaPlayer != null) mediaPlayer.release();
        executor.shutdown();
    }
}
