package model.player;

import model.enums.TotemColor;


public class Player {
    private final String name;
    private Totem totem;
    private final Tribe tribe;
    private int food;
    private int prestigePoints;
    private boolean isImmuneToShaman;
    private boolean hasDoublePointsForShaman;

    public Player(String name) {
        this.name = name;
        this.prestigePoints = 0;
        this.food = 0;
        this.totem = null;
        this.tribe = new Tribe();
    }


    // -- food --
    public int getFood(){ return food; }
    public void addFood(int amount){ food += amount; }

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
    public int getPrestigePoints(){ return prestigePoints; }
    public void addPrestigePoints(int amount){ prestigePoints += amount; }
    public void removePrestigePoints(int amount){ prestigePoints -= amount; }


    // The Tribe is directly exposed: the Controller queries it and adds cards through
    // tribe.addCharacter() or tribe.addBuilding()
    public Tribe getTribe(){ return tribe; }

    public Totem getTotem() {
        return totem;
    }

    public void setTotem(Totem totem) {
        this.totem = totem;
    }

    public boolean isImmuneToShaman() { return isImmuneToShaman; }

    public boolean isHasDoublePointForShaman() { return hasDoublePointForShaman; }

    public void setHasDoublePointsForShaman(boolean hasDoublePointsForShaman) {
        this.hasDoublePointsForShaman = hasDoublePointsForShaman;
    }

    public void setImmuneToShaman(boolean immuneToShaman) {
        isImmuneToShaman = immuneToShaman;
    }


    public String getName(){ return name; }
    public TotemColor getColor(){}
}