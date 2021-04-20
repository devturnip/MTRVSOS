import jade.Boot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import power.PowerStoreDisAgent;
import utils.ElasticHelper;
import utils.Settings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;

public class SmartGridRunner {
    private static Logger LOGGER = LoggerFactory.getLogger(SmartGridRunner.class);

    public static void main(String[] args) {
        String[] param = new String[ 6 ];
        param[ 0 ] = "-name";
        param[ 1 ] = "the-platform";
        param[ 2 ] = "-agents";
        param[ 3 ] = "tony:HelloWorldAgent";
        param[ 4 ] = "-port";
        param[ 5 ] = "0";

        Settings settingsInstance = Settings.getSettingsInstance();
        ElasticHelper elasticHelperInstance = ElasticHelper.getElasticHelperInstance();

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
                } else if (arguments.equals("-wt")){
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setMSToWait(Integer.parseInt(args[i+1]));
                } else if (arguments.equals("-testrun")) {
                    parsed.add((args[i]));
                    String index_name = "smartgridsos-" + LocalDateTime.now();
                    settingsInstance.setINDEXNAME(index_name.replaceAll(":", "-").toLowerCase(Locale.ROOT));
                } else if (arguments.equals("-indexname")) {
                    parsed.add((args[i]));
                    parsed.add(args[i+1]);
                    settingsInstance.setINDEXNAME(args[i+1]);
                }
            }

            if (parsed.size() != args.length) {
                LOGGER.warn("SOME ARGS NOT RECOGNISED!");
            }
        }

        try {
            LOGGER.info(settingsInstance.printSettings());
            if (elasticHelperInstance.initElasticClient(settingsInstance.getELASTIC_HOST(), settingsInstance.getELASTIC_PORT()) == true) {
                settingsInstance.setUseElastic(true);
            }
            Boot.main(param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
