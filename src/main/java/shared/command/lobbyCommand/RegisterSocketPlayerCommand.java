package shared.command.lobbyCommand;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

/**
 * Comando server-interno di registrazione di un player socket. Lo crea il
 * {@code ConnectionHandshaker} (sul thread per-connessione) dopo aver letto il
 * {@code ConnectMessage}, e lo impila sulla coda del {@code LobbyManager}, che
 * lo processa via {@link LobbyCommandVisitor#visit(RegisterSocketPlayerCommand)}:
 * sul lobby-thread il check-e-registra del nome è un'unica azione atomica
 * (la serializzazione della coda È la mutua esclusione), eliminando la race
 * TOCTOU sui nomi duplicati.
 * <p>
 * Il comando porta {@code socket}/{@code in}/{@code out} così che, <strong>solo
 * se</strong> il nome è libero, il lobby-thread costruisca
 * {@code SocketVirtualView}/{@code SocketClientHandler}/{@code SocketPlayerEntry}
 * e avvii il thread reader. Costruire dentro il ramo "nome libero" evita di
 * allocare il sender executor del player su un nome rifiutato e tiene il socket
 * aperto per il retry. Nessuna di queste operazioni è bloccante
 * ({@code Thread.start()} ritorna subito), quindi il lobby-thread non fa mai I/O.
 * <p>
 * {@code future} è il canale di risposta "ask": il lobby-thread lo completa con
 * {@code true} (ACCEPT, reader già avviato) o {@code false} (REJECT, nome
 * occupato); il thread di connessione vi blocca sopra con timeout.
 * <p>
 * <strong>Non viaggia mai sul wire</strong>: {@code socket}/{@code in}/{@code out}
 * e {@code future} non sono {@link java.io.Serializable}, ma il record nasce e
 * muore nella stessa JVM (stesso contratto di {@code PlayerReconnectedCommand}).
 */
public record RegisterSocketPlayerCommand(String playerName, Socket socket, ObjectInputStream in, ObjectOutputStream out,
                                          CompletableFuture<Boolean> future) implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
