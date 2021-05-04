import jade.Boot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ElasticHelper;
import utils.Settings;
import utils.UXE;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class SmartGridRunner {
    private static Logger LOGGER = LoggerFactory.getLogger(SmartGridRunner.class);

    public static void main(String[] args) throws Exception {

        Thread.setDefaultUncaughtExceptionHandler(new UXE());

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
                if (arguments.equalsIgnoreCase("-p")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setNumPowerAgents(Integer.parseInt(args[i + 1]));
                } else if (arguments.equalsIgnoreCase("-pd")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setNumPowerDisAgents(Integer.parseInt(args[i + 1]));
                } else if (arguments.equalsIgnoreCase("-sh")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setNumSmartHomeAgents(Integer.parseInt(args[i + 1]));
                } else if (arguments.equalsIgnoreCase("-ev")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setNumEVAgents(Integer.parseInt(args[i + 1]));
                } else if (arguments.equalsIgnoreCase("-rt")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setSecondsToRun(Integer.parseInt(args[i + 1]));
                } else if (arguments.equalsIgnoreCase("-wt")){
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setMSToWait(Integer.parseInt(args[i+1]));
                } else if (arguments.equalsIgnoreCase("-testrun")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    String value = args[i+1];
                    if (value.equalsIgnoreCase("consistentreliabilitythreshold")) {
                        settingsInstance.setTestRun(value);
                    }
                } else if (arguments.equalsIgnoreCase("-indexname")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setINDEXNAME(args[i+1]);
                } else if (arguments.equalsIgnoreCase("-elastichost")) {
                    parsed.add(args[i]);
                    parsed.add(args[i+1]);
                    settingsInstance.setELASTIC_HOST(args[i+1]);
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
            LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
            logArgs.put("action", "main.exit");
            logArgs.put("exit_code", "1");
            logArgs.put("exit_stacktrace", e.getStackTrace().toString());
            elasticHelperInstance.indexLogs(SmartGridRunner.class, logArgs);
            System.exit(1);
        }
    }
}
