import jade.Boot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import power.PowerStoreDisAgent;
import utils.Settings;

public class SmartGridRunner {
    private static Logger LOGGER = LoggerFactory.getLogger(SmartGridRunner.class);

    public static void main(String[] args) {
        String[] param = new String[ 7 ];
        param[ 0 ] = "-gui";
        param[ 1 ] = "-name";
        param[ 2 ] = "the-platform";
        param[ 3 ] = "-agents";
        param[ 4 ] = "tony:HelloWorldAgent";
        param[ 5 ] = "-port";
        param[ 6 ] = "0";

        if(args.length > 0) {
            Settings settingsInstance = Settings.getSettingsInstance();
            for (int i=0; i< args.length;i++){
                String arguments = args[i];
                switch (arguments){
                    case "-p":
                        settingsInstance.setNumPowerAgents(Integer.parseInt(args[i+1]));
                        break;
                    case "-pd":
                        settingsInstance.setNumPowerDisAgents(Integer.parseInt(args[i+1]));
                        break;
                    case "-sh":
                        settingsInstance.setNumSmartHomeAgents(Integer.parseInt(args[i+1]));
                        break;
                    case "-ev":
                        settingsInstance.setNumEVAgents(Integer.parseInt(args[i+1]));
                        break;
                    default:
                        LOGGER.warn("args not recognised.");
                }
            }
        }

        try {
            Boot.main(param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
