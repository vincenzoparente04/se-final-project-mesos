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

    // viene passato il cibo e il moltiplicatore di aura da pagare (se non ho abbastanza cibo), il metodo
    // controlla se ho abbastanza cibo -> false allora toglie tutto il cibo disponibile e chiama remove aura
    // usando il moltiplicatore per le restanti risorse da togliere
    // quando devo pagare solo cibo il multiplier = 0
    public void removeFood(int amount, int prestigePointsMultiplier) {
        int temp = getFood();
        if(amount <= temp) { food -= amount; }
        else {
            food -= temp;
            removePrestigePoints((amount - temp) * prestigePointsMultiplier);
        }

    }

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