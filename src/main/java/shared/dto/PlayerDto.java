package shared.dto;

public class PlayerDto {
    public final String name;
    public final int food;
    public final int prestigePoints;
    public final String color;          // TotemColor.name()
    public final String totemLocation;  // TotemLocation.name()
    public final TribeDto tribe;

    public PlayerDto(String name, int food, int prestigePoints,
                     String color, String totemLocation, TribeDto tribe) {
        this.name = name;
        this.food = food;
        this.prestigePoints = prestigePoints;
        this.color = color;
        this.totemLocation = totemLocation;
        this.tribe = tribe;
    }
}
