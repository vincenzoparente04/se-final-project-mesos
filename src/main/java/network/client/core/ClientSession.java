package network.client.core;

/**
 * Immutable snapshot of the local player's session.
 * Set once after a successful connection; never mutated afterwards.
 *
 * @param playerName the local player's registered name
 * @param virtualServer the connected proxy used to talk to the server
 */
public record ClientSession(String playerName, VirtualServer virtualServer) {}
