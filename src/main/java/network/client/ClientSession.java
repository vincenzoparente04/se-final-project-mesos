package network.client.core;

/**
 * Immutable snapshot of the local player's session.
 * Set once after a successful connection; never mutated afterwards.
 */
public record ClientSession(String playerName, VirtualServer virtualServer) {}
