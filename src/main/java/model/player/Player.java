package model.player;

import model.enums.TotemColor;


public class Player {
    private final String name;
    private final Totem totem;
    private final Tribe tribe;
    private int food;
    private int prestigePoints;


    // -- food --
    public int getFood()
    public void addFood(int amount)

    // Controller checks that the player has enough food before calling removeFood
    public void removeFood(int amount)

    // -- prestige points --
    public int getPrestigePoints()
    public void addPrestigePoints(int amount)
    public void removePrestigePoints(int amount)


    // The Tribe is directly exposed: the Controller queries it and adds cards through
    // tribe.addCharacter() or tribe.addBuilding()
    public Tribe getTribe()

    public Totem getTotem()
    public String getName()
    public TotemColor getColor()
}