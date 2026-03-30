package model.player;

import model.enums.TotemColor;
import model.enums.TotemLocation;


public class Player {
    private final String name;
    private final Tribe tribe;
    private int food;
    private int prestigePoints;
    private TotemLocation totemLocation;
    private TotemColor color;

    private boolean shamanicImmunity = false;
    private boolean shamanicBonusStars = false;
    private boolean shamanicDoublePrestige = false;
    private boolean extraFoodOnTotemReturn = false;
    private boolean extraDraw = false;

// setters e getters per tutti e tre

    public Player(String name) {
        this.name = name;
        this.prestigePoints = 0;
        this.food = 0;
        this.tribe = new Tribe();
        this.totemLocation = TotemLocation.TURN_ORDER_TILE; // default location at the start of the game
        this.color = null;
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

    // boolean getters
    public boolean hasShamanicImmunity() { return shamanicImmunity; }
    public boolean hasShamanicDoublePrestige() { return shamanicDoublePrestige; }
    public boolean hasShamanicBonusIcons() { return shamanicBonusStars; }
    public boolean hasExtraFoodOnTotemReturn() { return extraFoodOnTotemReturn; }
    public boolean hasExtraDraw() { return extraDraw; }

    // boolean setters
    public void setShamanicImmunity(boolean shamanicImmunity) {
        this.shamanicImmunity = shamanicImmunity;
    }
    public void setShamanicBonusIcons(boolean shamanicBonusStars) {
        this.shamanicBonusStars = shamanicBonusStars;
    }
    public void setShamanicDoublePrestige(boolean shamanicDoublePrestige) {
        this.shamanicDoublePrestige = shamanicDoublePrestige;
    }
    public void setExtraFoodOnTotemReturn(boolean extraFoodOnTotemReturn) {
        this.extraFoodOnTotemReturn = extraFoodOnTotemReturn;
    }
    public void setExtraDraw(boolean extraDraw) {
        this.extraDraw = extraDraw;
    }

    public String getName(){ return name; }
    public TotemColor getColor() { return color; }
    public TotemLocation getLocation() { return totemLocation; }

    public void setLocation(TotemLocation location){
        this.totemLocation = location;
    }
    public void setColor(TotemColor color) { this.color = color; }
}