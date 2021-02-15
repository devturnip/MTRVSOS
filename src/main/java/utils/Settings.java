package utils;

public class Settings {
    private Settings(){}
    private static Settings settingsInstance = new Settings();

    //Rate Flags
    private int rateSecsPowerGen = 1000;
    private int rateSecsSmartHome = 1000;
    private int houseUnit = 100;
    private int secondsToRun = 60;

    //UI Size
    private int multiplier = 2;
    private int imageHeightXY = 30;
    private int homeImageXY = 20;
    private int evImageXY = 15;
    private double canvasX = 1024;
    private double canvasY = 768;

    //Agent Flags
    private int numPowerAgents = 2;
    private int numPowerDisAgents = 5;
    private int numSmartHomeAgents = 5;
    private int numEVAgents = 2;

    //other
    private String PORT_NAME = "7778";
    private String HOSTNAME = "localhost";

    public static Settings getSettingsInstance(){return settingsInstance;}
    public int getRateSecsPowerGen(){return rateSecsPowerGen;};
    public int getRateSecsSmartHome(){return rateSecsSmartHome;};
    public int getHouseUnit(){return houseUnit;}
    public double getCanvasX(){return canvasX;}
    public double getCanvasY(){return canvasY;}
    public int getSecondsToRun(){return secondsToRun;};

    //UI
    public int getMultiplier(){return multiplier;};
    public int getImageHeightXY(){return imageHeightXY;};
    public int getHomeImageXY(){return homeImageXY;};
    public int getEvImageXY(){return evImageXY;};
    public int getNumPowerAgents(){return numPowerAgents;};
    public int getNumPowerDisAgents(){return numPowerDisAgents;};
    public int getNumSmartHomeAgents(){return numSmartHomeAgents;};
    public int getNumEVAgents(){return numEVAgents;};

    //other
    public String getPORT_NAME(){return PORT_NAME;};
    public String getHOSTNAME(){return HOSTNAME;};
}
