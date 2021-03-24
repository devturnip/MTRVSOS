import jade.Boot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import power.PowerStoreDisAgent;
import utils.Settings;

import java.util.ArrayList;

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

        Settings settingsInstance = Settings.getSettingsInstance();
        ArrayList<String> parsed = new ArrayList<>();

        if(args.length > 0) {
            for (int i=0; i< args.length;i++){
                String arguments = args[i];
                if (arguments.equals("-p")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setNumPowerAgents(Integer.parseInt(args[i + 1]));
                } else if (arguments.equals("-pd")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setNumPowerDisAgents(Integer.parseInt(args[i + 1]));
                } else if (arguments.equals("-sh")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setNumSmartHomeAgents(Integer.parseInt(args[i + 1]));
                } else if (arguments.equals("-ev")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setNumEVAgents(Integer.parseInt(args[i + 1]));
                } else if (arguments.equals("-rt")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setSecondsToRun(Integer.parseInt(args[i + 1]));
                }
            }

            if (parsed.size() != args.length) {
                LOGGER.warn("SOME ARGS NOT RECOGNISED!");
            }
        }

        try {
            LOGGER.info(settingsInstance.printSettings());
            Boot.main(param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
