package network.client;

public interface VirtualServer {

    void sendChooseColor(String color);

    void sendPlaceTotem(char tile);

    void sendDrawCard(int cardId);

    void sendEndTurn();

    void sendCreateLobby(int maxPlayers);

    void sendJoinLobby(String lobbyId);

    void sendListLobbies();

    void close();
}
