package shared.message;

import java.io.Serializable;

public record ConnectMessage(String playerName) implements Serializable {}
