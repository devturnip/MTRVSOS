package utils;

public class Settings {
    private Settings(){}
    private static Settings settingsInstance = new Settings();

    //Rate Flags
    private int rateSecsPowerGen = 1000;
    private int rateSecsSmartHome = 1000;
    private int rateSecsEV = 2000;
    private int houseUnit = 1000;
    private int secondsToRun = 600;

    //UI Size
    private int multiplier = 2;
    private int imageHeightXY = 30;
    private int homeImageXY = 20;
    private int evImageXY = 15;
    private double canvasX = 1024;
    private double canvasY = 768;

    private boolean pauseSimulation = false;
    private int simCheckRate = 200;

    //Agent Flags
    private int numPowerAgents = 1;
    private int numPowerDisAgents = 1;
    private int numSmartHomeAgents = 1;
    private int numEVAgents = 0;

    private double InitCapacityFactor = 0.70;

    //SoSAgent Management Flags
    private double preferredUtilisationRate = 85;
    private double preferredIncrement = 0.02;
    private double powerUtilisationRate = 90;

    //other
    private String PORT_NAME = "7778";
    private String HOSTNAME = "localhost";

    public static Settings getSettingsInstance(){return settingsInstance;}
    public int getRateSecsPowerGen(){return rateSecsPowerGen;}
    public int getRateSecsSmartHome(){return rateSecsSmartHome;}
    public int getHouseUnit(){return houseUnit;}
    public double getCanvasX(){return canvasX;}
    public double getCanvasY(){return canvasY;}
    public int getSecondsToRun(){return secondsToRun;}

    public double getPowerUtilisationRate() { return powerUtilisationRate;}
    public double getPreferredIncrement() { return preferredIncrement;}
    public double getPreferredUtilisationRate() { return preferredUtilisationRate;}

    public double getInitCapacityFactor() { return InitCapacityFactor; }

    public boolean getSimulationState() {
        return pauseSimulation;
    }
    public void setPauseSimulation(boolean state) {
        pauseSimulation = state;
    }
    public int getSimCheckRate(){return simCheckRate;}

    //UI
    public int getMultiplier(){return multiplier;}
    public int getImageHeightXY(){return imageHeightXY;}
    public int getHomeImageXY(){return homeImageXY;}
    public int getEvImageXY(){return evImageXY;}
    public int getNumPowerAgents(){return numPowerAgents;}
    public int getNumPowerDisAgents(){return numPowerDisAgents;}
    public int getNumSmartHomeAgents(){return numSmartHomeAgents;}
    public int getNumEVAgents(){return numEVAgents;}

    //other
    public String getPORT_NAME(){return PORT_NAME;}
    public String getHOSTNAME(){return HOSTNAME;}
}
