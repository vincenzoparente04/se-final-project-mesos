package model.player;

import model.enums.TotemColor;
import model.enums.TotemLocation

public class Totem {
    private final Player owner;
    // where it is physically located: on the TurnOrderTile or on which OrderTile
    private TotemLocation location;
    private final TotemColor color;

    //constructor
    public Totem(Player owner, TotemColor color) {
        this.owner = owner;
        this.color = color;
    }

    //getter
    public Player getOwner()
    public TotemColor getColor()
    public TotemLocation getLocation()

    //setter
    public void setLocation(TotemLocation location){
        this.location = location;
    }
}