package consumer;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import power.Power;
import utils.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class SmartHomeAgent extends Agent{
    Settings settingsInstance = Settings.getSettingsInstance();
    private HashMap<String, Double> appliancesList = new HashMap<>();
    private double totalAppliancePowerConsumption = 0;
    private int rateSecs = settingsInstance.getRateSecsSmartHome();
    private boolean hasInit = false;

    private double agent_X = 0;
    private double agent_Y = 0;
    private ImageView agentImageView;
    private Label agentLabel;
    private int houseUnit = settingsInstance.getHouseUnit(); //represents number of houses per agent
    private boolean pauseAgent = false;

    private Maps mapsInstance = Maps.getMapsInstance();
    private Utils utility = new Utils();
    private Power powerInstance = Power.getPowerInstance();

    private LinkedHashMap<AID, Double> nearestNeighbours = new LinkedHashMap<>();
    private Map.Entry<AID, Double> nearestNeighbour = null;
    private ArrayList<Behaviour> behaviourList = new ArrayList<>();

    //message sending flags
    private AID currentNeighbour;
    private int retryCount = 0;
    private boolean messageSent = false;
    private int rejCount = 0;
    private int logError = 0;

    //colour flags
    private int currentColour = 0;
    private int ORANGE = 3;
    private int YELLOWGREEN = 4;

    //logs
    private static Logger LOGGER = LoggerFactory.getLogger(SmartHomeAgent.class);
    private static ElasticHelper elasticHelper = ElasticHelper.getElasticHelperInstance();

    @Override
    protected void setup() {
        super.setup();
        initAppliances();
        LOGGER.info(getLocalName()+ "'s total appliances power consumption: "+ totalAppliancePowerConsumption);

        LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
        logArgs.put("action", "smarthome.init");
        logArgs.put("power_consumption", String.valueOf(totalAppliancePowerConsumption));
        logArgs.put("consumption_rate", String.valueOf(rateSecs));
        elasticHelper.indexLogs(this, logArgs);

        InitPosition initPosition = new InitPosition(this, settingsInstance.getMSToWait());
        ReceiveMessage receiveMessage = new ReceiveMessage();
        WakeConsumer wakeConsumer = new WakeConsumer(this, settingsInstance.getMSToWait()+2000);
        CheckSimulationState checkSimulationState = new CheckSimulationState(this, settingsInstance.getSimCheckRate());
        behaviourList.add(initPosition);
        behaviourList.add(receiveMessage);
        behaviourList.add(wakeConsumer);
        behaviourList.add(checkSimulationState);
        addBehaviour(initPosition);
        addBehaviour(receiveMessage);
        addBehaviour(wakeConsumer);
        addBehaviour(checkSimulationState);
    }

    @Override
    protected void takeDown(){
        super.takeDown();
        powerInstance.subtractDemand(totalAppliancePowerConsumption);
        setDemandLabel();
        String message = getLocalName() + " takedown. Killing...";
        LOGGER.info(message);
        elasticHelper.indexLogs(this, message);

        try { DFService.deregister(this); }
        catch (Exception e) {}
        if(!behaviourList.isEmpty()) {
            String log= "";
            for (Behaviour b: behaviourList){
                log = getLocalName() + " Removing behaviour(s): "+ b.getBehaviourName();
                LOGGER.info(log);
                elasticHelper.indexLogs(this, log);
                removeBehaviour(b);
            }
        }
        mapsInstance.removeUI(agentImageView);
        mapsInstance.removeUI(agentLabel);
        doDelete();
    }

    private void initAppliances() {
        Appliances appliances = new Appliances(10);
        appliancesList = appliances.initAppliances();
        Iterator iterator = appliancesList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            totalAppliancePowerConsumption = totalAppliancePowerConsumption + (Double)pair.getValue();
        }
        totalAppliancePowerConsumption = totalAppliancePowerConsumption * houseUnit;
        powerInstance.addDemand(totalAppliancePowerConsumption);
        setDemandLabel();
    }

    private void setDemandLabel() {
        HashMap<String, Label> demandHM = mapsInstance.getDemandGenLabel();
        Label demandRate = demandHM.get("demandRate");
        double demandLevel = powerInstance.getDemand();
        BigDecimal bd = new BigDecimal(demandLevel).setScale(2, RoundingMode.HALF_UP);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                demandRate.setText("D (kwh/s): " + bd.doubleValue());
            }
        });
    }

    private void initPosition() {
        HashMap<String, ImageView> hm = mapsInstance.getAgentMap(this.getLocalName(), true);
        HashMap.Entry<String, ImageView> entry = hm.entrySet().iterator().next();
        String agentName = entry.getKey();
        ImageView iv = entry.getValue();
        agentImageView = iv;
        agent_X = iv.getX();
        agent_Y = iv.getY();
        LOGGER.debug("THIS:" + this.getLocalName() + " agent:" + agentName + " X:" + agent_X + " Y:" + agent_Y);

        HashMap<String, Label> lm = mapsInstance.getAgentLabelMap(this.getLocalName());
        Map.Entry<String, Label> labelEntry = lm.entrySet().iterator().next();
        agentLabel = labelEntry.getValue();
    }

    private class InitPosition extends WakerBehaviour {
        public InitPosition(Agent a, long timeout) {
            super(a, timeout);
        }
        @Override
        protected void onWake() {
            super.onWake();
            initPosition();

            String[] servicesArgs = new String[] {"Power-Storage_Distribution", "Power-Generation"};
            nearestNeighbours = utility.getNearestObjectsList(this.myAgent, agent_X, agent_Y, servicesArgs);
            nearestNeighbour = utility.getNearest(this.myAgent, agent_X, agent_Y, servicesArgs);
            currentNeighbour = nearestNeighbour.getKey();

            hasInit = true;
        }
    }

    private class WakeConsumer extends WakerBehaviour {
        public WakeConsumer(Agent a, long timeout) {
            super(a, timeout);
        }

        @Override
        protected void onWake() {
            super.onWake();
            ConsumeElectricity consumeElectricity = new ConsumeElectricity(myAgent, rateSecs);
            behaviourList.add(consumeElectricity);
            addBehaviour(consumeElectricity);
        }
    }

    private class ConsumeElectricity extends TickerBehaviour {

        public ConsumeElectricity(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (pauseAgent == false) {
                HashMap.Entry<String, String> arguments = new HashMap.SimpleEntry<String, String>("toConsume", String.valueOf(totalAppliancePowerConsumption));
                if (hasInit == true) {
                    if (messageSent == false) {
                        utility.sendMessageWithArgs(myAgent, currentNeighbour, arguments, "BEGIN_CONSUME", "REQUEST");
                        messageSent = true;
                    } else if (messageSent) {
                        retryCount = retryCount + 1;
                        LOGGER.debug("BLOCK");
                        if (retryCount == 5) {
                            if (logError>=3) {
                                LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
                                logArgs.put("action", "smarthome.power_request");
                                logArgs.put("request_power", "no_reply_5t");
                                logArgs.put("sent_request_to", currentNeighbour.getLocalName());
                                elasticHelper.indexLogs(myAgent, logArgs);
                                logError=0;
                            }
                            logError+=1;

                            //if blocked more than 5 times, recheck for nearest power source again.
                            if (currentColour != ORANGE) {
                                try {
                                    mapsInstance.changeColor(agentImageView, "ORANGE");
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                currentColour = ORANGE;
                            }
                            retryCount = 0;
                            String[] servicesArgs = new String[]{"Power-Storage_Distribution", "Power-Generation"};
                            currentNeighbour = utility.getNearestObjectsList(this.myAgent, agent_X, agent_Y, servicesArgs).keySet().iterator().next();
                            messageSent = false;
                        }
                        block();
                    }
                }
            } else if (pauseAgent) {
                block();
            }
        }
    }

    private class CheckSimulationState extends TickerBehaviour{
        public CheckSimulationState(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            pauseAgent = settingsInstance.getSimulationState();
        }
    }



    private class ReceiveMessage extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg!=null) {
                String content = msg.getContent();
                switch (content) {
                    case "PAUSE":
                        pauseAgent = true;
                        break;
                    case "RESUME" :
                        pauseAgent = false;
                        break;
                    case "ACCEPT_CONSUME":
                        messageSent = false;
                        if (currentColour != YELLOWGREEN) {
                            try {
                                mapsInstance.changeColor(agentImageView, "YELLOWGREEN");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            currentColour = YELLOWGREEN;
                        }

                        LinkedHashMap<String, String> logArgs0 = new LinkedHashMap<>();
                        logArgs0.put("action", "smarthome.power_receive");
                        logArgs0.put("receive_power", "true");
                        logArgs0.put("accepted_by", msg.getSender().getLocalName());
                        elasticHelper.indexLogs(myAgent, logArgs0);
                        rejCount = 0;

                        break;
                    case "REJECT_CONSUME":

                        messageSent = false;
                        //change status to orange
                        if (currentColour != ORANGE) {
                            try {
                                mapsInstance.changeColor(agentImageView, "ORANGE");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            currentColour = ORANGE;
                        }

                        if (rejCount >= 2) {
                            LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
                            logArgs.put("action", "smarthome.power_receive");
                            logArgs.put("receive_power", "false");
                            logArgs.put("rejected_by", msg.getSender().getLocalName());
                            elasticHelper.indexLogs(myAgent, logArgs);
                            rejCount = 0;
                        }
                        rejCount += 1;

                        //get next neighbour in list to send
                        HashMap.Entry<String, String> arguments = new HashMap.SimpleEntry<String, String>("toConsume", String.valueOf(totalAppliancePowerConsumption));
                        String[] servicesArgs = new String[] {"Power-Storage_Distribution", "Power-Generation"};
                        Iterator consumptionEngine = utility.getNearestObjectsList(this.myAgent, agent_X, agent_Y, servicesArgs).keySet().iterator();
                        AID first = utility.getNearestObjectsList(this.myAgent, agent_X, agent_Y, servicesArgs).keySet().iterator().next();
                        while (consumptionEngine.hasNext()) {
                            AID temp = (AID) consumptionEngine.next();
                            if (currentNeighbour.getLocalName().equals(temp.getLocalName()) && consumptionEngine.hasNext()){
                                currentNeighbour = (AID) consumptionEngine.next();
                                break;
                            } else if (!consumptionEngine.hasNext()) {
                                currentNeighbour = first;
                                break;
                            }
                        }
                        break;
                }
            }
            else {
                block();
            }
        }
    }
}
