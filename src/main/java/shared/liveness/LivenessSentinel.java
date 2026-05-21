package shared.liveness;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Astrazione bidirezionale di liveness che incapsula un sender periodico e un watchdog.
 *
 * <p>Il <em>sender</em> esegue {@code sendHeartbeatAction} ogni {@code sendIntervalMs} ms.
 * Il <em>watchdog</em> controlla ogni {@code checkIntervalMs} ms se dall'ultimo heartbeat
 * inbound è trascorso più di {@code timeoutMs} ms; in tal caso invoca {@code onTimeoutAction}
 * <strong>una sola volta</strong> e si ferma.
 *
 * <h2>Canale di liveness isolato</h2>
 * {@link #notifyInbound()} deve essere chiamato <em>esclusivamente</em> dagli handler dei
 * messaggi heartbeat dedicati (es. {@code HeartbeatCommand} lato server,
 * {@code HeartbeatMessage} lato client). Il traffico applicativo (stati di gioco, comandi di
 * lobby, ecc.) non aggiorna il timestamp di liveness. Questo garantisce che un blocco del
 * traffico applicativo non mascheri un server/client morto, e che un silenzio sugli heartbeat
 * non venga compensato da messaggi applicativi intensi.
 *
 * <h2>Invariante temporale</h2>
 * {@code timeoutMs > 2 * sendIntervalMs} è necessaria per tollerare il jitter di scheduling
 * e ritardi temporanei dovuti a messaggi grandi che impegnano il sender. Con i valori di
 * default ({@code sendIntervalMs=2s}, {@code timeoutMs=10s}) il detection time massimo è
 * {@code timeoutMs + checkIntervalMs = 12s}.
 *
 * <h2>Thread safety</h2>
 * Questa classe è thread-safe. {@link #notifyInbound()} può essere chiamato da qualunque
 * thread in qualunque momento; il costo è O(1) senza lock (scrittura su campo {@code volatile}).
 * {@link #start()} e {@link #stop()} sono idempotenti.
 *
 * <h2>Agnosticismo</h2>
 * La classe non importa nulla da {@code network.*}, {@code shared.message.*} o
 * {@code shared.command.*}. L'accoppiamento avviene esclusivamente tramite le callback
 * passate al costruttore.
 */
public class LivenessSentinel {

    private final long sendIntervalMs;
    private final long checkIntervalMs;
    private final long timeoutMs;
    private final Runnable sendHeartbeatAction;
    private final Runnable onTimeoutAction;
    private final String threadNameSuffix;

    /** Timestamp (millis epoch) dell'ultimo messaggio inbound ricevuto. */
    private volatile long lastInbound;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    /** Garantisce che onTimeoutAction venga invocata al più una volta. */
    private final AtomicBoolean timedOut = new AtomicBoolean(false);

    private ScheduledExecutorService executor;

    /**
     * Costruisce un sentinel non ancora avviato.
     *
     * @param threadNameSuffix    suffisso usato per nominare il thread interno
     *                            ({@code "liveness-" + suffix})
     * @param sendIntervalMs      periodo di invio heartbeat in millisecondi
     * @param checkIntervalMs     periodo di controllo watchdog in millisecondi
     * @param timeoutMs           soglia di silenzio oltre la quale la connessione è dichiarata
     *                            persa; deve essere {@code > 2 * sendIntervalMs}
     * @param sendHeartbeatAction callback invocata periodicamente per inviare un heartbeat;
     *                            le eccezioni vengono silenziate (lo stream potrebbe essere già
     *                            chiuso: il watchdog è il giudice di liveness)
     * @param onTimeoutAction     callback invocata al più una volta quando il watchdog scatta;
     *                            può chiamare {@link #stop()} sullo stesso sentinel senza
     *                            deadlock, perché {@code stop()} non acquisisce nessun lock
     *                            interno
     */
    public LivenessSentinel(
            String threadNameSuffix,
            long sendIntervalMs,
            long checkIntervalMs,
            long timeoutMs,
            Runnable sendHeartbeatAction,
            Runnable onTimeoutAction) {
        this.threadNameSuffix = threadNameSuffix;
        this.sendIntervalMs = sendIntervalMs;
        this.checkIntervalMs = checkIntervalMs;
        this.timeoutMs = timeoutMs;
        this.sendHeartbeatAction = sendHeartbeatAction;
        this.onTimeoutAction = onTimeoutAction;
    }

    /**
     * Avvia sender e watchdog. Idempotente: chiamate successive a {@code start()} sono no-op.
     * {@code lastInbound} viene inizializzato al momento di questa chiamata.
     */
    public void start() {
        if (!started.compareAndSet(false, true)) return;
        lastInbound = System.currentTimeMillis();

        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "liveness-" + threadNameSuffix);
            t.setDaemon(true);
            return t;
        });

        executor.scheduleAtFixedRate(this::senderTick, sendIntervalMs, sendIntervalMs, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::watchdogTick, checkIntervalMs, checkIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Notifica il sentinel che un messaggio inbound è arrivato, aggiornando il timestamp
     * usato dal watchdog. Costo O(1), nessun lock.
     */
    public void notifyInbound() {
        lastInbound = System.currentTimeMillis();
    }

    /**
     * Ferma sender e watchdog. Idempotente: usa {@link AtomicBoolean#compareAndSet} per
     * garantire safe-by-default su doppia chiamata.
     */
    public void stop() {
        if (!stopped.compareAndSet(false, true)) return;
        if (executor != null) executor.shutdownNow();
    }

    private void senderTick() {
        if (stopped.get()) return;
        try {
            sendHeartbeatAction.run();
        } catch (Exception ignored) {
            // Se la callback fallisce, lo stream è probabilmente già chiuso.
            // Il watchdog è il giudice unico di liveness: non propaghiamo l'eccezione.
        }
    }

    private void watchdogTick() {
        if (stopped.get()) return;
        long now = System.currentTimeMillis();
        if (now - lastInbound > timeoutMs) {
            if (timedOut.compareAndSet(false, true)) {
                onTimeoutAction.run();
            }
        }
    }
}
