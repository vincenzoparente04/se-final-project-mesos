package shared.message;

import java.io.Serializable;

/**
 * Handshake message sent by the socket client right after the connection is
 * established, before entering the command flow: it communicates the name the
 * player wants to register with. It does not belong to the {@link ServerMessage}
 * hierarchy because it travels client→server.
 *
 * @param playerName name requested by the player
 */
public record ConnectMessage(String playerName) implements Serializable {}
