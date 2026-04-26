package server.socket;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Groups the I/O streams already opened for one connected player.
 * Created by {@link server.core.LobbyManager} and passed directly to
 * {@link SocketPlayerEntry} so that the same {@link BufferedReader} (and its
 * internal buffer) is reused throughout the player's lifetime — preventing
 * data loss caused by creating a second reader on the same socket input stream.
 */
public record PlayerConnection(String name, Socket socket, BufferedReader in, PrintWriter out) {}
