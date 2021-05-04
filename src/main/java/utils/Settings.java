package utils;

public class Settings {
    private Settings(){}
    private static Settings settingsInstance = new Settings();

    //Rate Flags
    private int rateSecsPowerGen = 500;
    private int rateSecsSmartHome = 600;
    private int rateSecsEV = 2000;
    private int houseUnit = 1000;
    private int secondsToRun = 600;
    private int MSToWait = 4000;

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
    private int numEVAgents = 1;

    private double InitCapacityFactor = 0.70;

    //SoSAgent Management Flags
    private double preferredUtilisationRate = 85;
    private double preferredIncrement = 0.02;
    private double powerUtilisationRate = 90;

    //other
    private String PORT_NAME = "7778";
    private String HOSTNAME = "localhost";

    private boolean useElastic = false;
    private String ELASTIC_HOST = "192.168.0.32";
//    private String ELASTIC_HOST = "192.168.25.19";
    private int ELASTIC_PORT = 9200;
    private String INDEXNAME = "smartgridsos";

    //mt flags
    private String testRun = "";

    //METHODS----------------------------------------------------------------
    public static Settings getSettingsInstance(){return settingsInstance;}
    public void setNumPowerAgents(int num){numPowerAgents=num;}
    public void setNumPowerDisAgents(int num){numPowerDisAgents=num;}
    public void setNumSmartHomeAgents(int num){numSmartHomeAgents=num;}
    public void setNumEVAgents(int num){numEVAgents=num;}
    public void setSecondsToRun(int num){secondsToRun=num;}
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
    public int getMSToWait(){return MSToWait;}
    public void setMSToWait(int ms){MSToWait = ms;}

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

    //elastic flags
    public boolean getUseElastic(){return useElastic;}
    public void setUseElastic(boolean elastic){useElastic=elastic;}
    public String getELASTIC_HOST(){return ELASTIC_HOST;}
    public int getELASTIC_PORT(){return ELASTIC_PORT;}
    public void setELASTIC_HOST(String HOST){ELASTIC_HOST=HOST;}
    public void setELASTIC_PORT(int PORT){ELASTIC_PORT=PORT;}
    public String getINDEXNAME(){return INDEXNAME;}
    public void setINDEXNAME(String indexname){INDEXNAME=indexname;}

    //mt
    public String getTestRun(){return testRun;}
    public void setTestRun(String value){testRun = value;}

    public String printSettings() {
        StringBuilder sb = new StringBuilder();
        sb.append("==============================\n");
        sb.append("starting program with settings:\n");
        sb.append("-----AGENT FLAGS-----");
        sb.append("\nNum of Power_Agents: " + getNumPowerAgents());
        sb.append("\nNum of Power_Distribution_Agents: " + getNumPowerDisAgents());
        sb.append("\nNum of Smart_Home_Agent: " + getNumSmartHomeAgents());
        sb.append("\nNum of EV_Agents: " + getNumEVAgents());
        sb.append("\nTick rate (ms) of Power_Agents: " + getRateSecsPowerGen());
        sb.append("\nTick rate (ms) of Smart_Home_Agents: " + getRateSecsSmartHome());
        sb.append("\nStart after x % power levels of SoS_Agent: " + getPowerUtilisationRate());
        sb.append("\n% to keep generation/demand of SoS_Agent: " + getPreferredUtilisationRate());
        sb.append("\n% of incr/decr level of SoS_Agent: " + getPreferredIncrement());
        sb.append("\n-----OTHERS-----");
        sb.append("\nTime (s) for simulator to run: " + getSecondsToRun());
        sb.append("\n==============================");
        return sb.toString();
    }
}
