package power;

public class Power {
    private static Power powerInstance = new Power();
    private double powerLevels = 0.0;
    private double gridMax = 0.0;
    private double demand = 0.0;
    private double genRate = 0.0;

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

    public void subtractGridMax(double maxCapacity) {
        gridMax = gridMax - maxCapacity;
    }

    public double getGridMax() {
        return gridMax;
    }

    public double getDemand() { return demand; }

    public void addDemand(double value) {
        demand = demand + value;
    }

    public void subtractDemand(double value) {
        double temp = demand-value;
        if (temp < 0) {
            demand = 0;
        } else {
            demand = demand - value;
        }
    }

    public double getGenRate() { return genRate; }

    public void addGenRate(double value) {
        genRate = genRate + value;
    }

    public void subtractGenRate(double value) {
        double temp = genRate - value;
        if (temp < 0) {
            genRate = 0;
        } else {
            genRate = genRate - value;
        }
    }
}
