package power;

public class Power {
    private static Power powerInstance = new Power();
    private double powerLevels = 0.0;

    private Power(){}

    public static Power getPowerInstance() {
        return powerInstance;
    }

    public double showPowerLevels() {
        return powerLevels;
    }

    public void addPowerLevel(double plvl) {
        powerLevels = powerLevels + plvl;
    }

    public void subtractPowerLevel(double plvl) {
        powerLevels = powerLevels - plvl;
    }
}
