package power;

public class Power {
    private static Power powerInstance = new Power();
    private double powerLevels = 0.0;
    private double gridMax = 0.0;

    private Power(){}

    public static Power getPowerInstance() {
        return powerInstance;
    }

    public double showPowerLevels() {
        return powerLevels;
    }

    public void addPowerLevel(double plvl) {
        double temp = powerLevels + plvl;
        if (temp > gridMax) {
            double toAdd  = gridMax - temp;
            powerLevels = powerLevels + toAdd;
        } else {
            powerLevels = powerLevels + plvl;
        }
    }

    public void subtractPowerLevel(double plvl) {
        double temp = powerLevels - plvl;
        if (temp < 0) {
            double toSubtract  = 0;
            powerLevels = powerLevels - toSubtract;
        } else {
            powerLevels = powerLevels - plvl;
        }
    }

    public void addGridMax(double maxCapacity) {
        gridMax = gridMax + maxCapacity;
    }

    public double getGridMax() {
        return gridMax;
    }
}
