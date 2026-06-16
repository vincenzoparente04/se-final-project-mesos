package shared.liveness;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bidirectional liveness abstraction encapsulating a periodic heartbeat sender
 * and a watchdog.
 *
 * The sender invokes {@code sendHeartbeatAction} every {@code sendIntervalMs} ms.
 * The watchdog checks every {@code checkIntervalMs} ms whether more than
 * {@code timeoutMs} ms have elapsed since the last inbound heartbeat; if so,
 * it invokes {@code onTimeoutAction} exactly once and stops.
 *
 * Isolated liveness channel: {@link #notifyInbound()} must be called exclusively
 * by the handlers of dedicated heartbeat messages (e.g. {@code HeartbeatCommand}
 * on the server side, {@code HeartbeatMessage} on the client side). Application
 * traffic (game states, lobby commands, etc.) does not update the liveness
 * timestamp. This ensures that a stall in application traffic does not mask a
 * dead server/client, and that silence on heartbeats is not compensated by heavy
 * application messaging.
 *
 * Timing invariant: {@code timeoutMs > 2 * sendIntervalMs} is required to
 * tolerate scheduling jitter and transient delays caused by large messages that
 * keep the sender busy. With the default values ({@code sendIntervalMs=2s},
 * {@code timeoutMs=10s}) the maximum detection time is
 * {@code timeoutMs + checkIntervalMs = 12s}.
 *
 * Thread safety: this class is thread-safe. {@link #notifyInbound()} may be
 * called from any thread at any time at O(1) cost with no lock (volatile field
 * write). {@link #start()} and {@link #stop()} are idempotent.
 *
 * Transport agnostic: this class imports nothing from {@code network.*},
 * {@code shared.message.*} or {@code shared.command.*}. Coupling happens
 * exclusively through the callbacks passed to the constructor.
 */
public class LivenessSentinel {

    private final long sendIntervalMs;
    private final long checkIntervalMs;
    private final long timeoutMs;
    private final Runnable sendHeartbeatAction;
    private final Runnable onTimeoutAction;
    private final String threadNameSuffix;

    /** Epoch timestamp (ms) of the last inbound message received. */
    private volatile long lastInbound;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    /** Ensures onTimeoutAction is invoked at most once. */
    private final AtomicBoolean timedOut = new AtomicBoolean(false);

    private ScheduledExecutorService executor;

    /**
     * Constructs a sentinel that is not yet started.
     *
     * @param threadNameSuffix    suffix used to name the internal thread
     *                            ({@code "liveness-" + suffix})
     * @param sendIntervalMs      heartbeat send interval in milliseconds
     * @param checkIntervalMs     watchdog check interval in milliseconds
     * @param timeoutMs           silence threshold beyond which the connection is declared dead;
     *                            must be {@code > 2 * sendIntervalMs}
     * @param sendHeartbeatAction callback invoked periodically to send a heartbeat;
     *                            exceptions are silenced (the stream may already be closed —
     *                            the watchdog is the sole arbiter of liveness)
     * @param onTimeoutAction     callback invoked at most once when the watchdog fires;
     *                            may call {@link #stop()} on this same sentinel without
     *                            deadlock, since {@code stop()} acquires no internal lock
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
     * Starts the sender and the watchdog. Idempotent: subsequent calls to
     * {@code start()} are no-ops. {@code lastInbound} is initialised to the
     * current time at this call.
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
     * Notifies the sentinel that an inbound message has arrived, updating the
     * timestamp used by the watchdog. O(1) cost, no lock.
     */
    public void notifyInbound() {
        lastInbound = System.currentTimeMillis();
    }

    /**
     * Stops the sender and the watchdog. Idempotent: uses
     * {@link AtomicBoolean#compareAndSet} to guarantee safety on repeated calls.
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
            // If the callback fails, the stream is likely already closed.
            // The watchdog is the sole arbiter of liveness: do not propagate.
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
