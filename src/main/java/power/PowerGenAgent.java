package power;

import com.opencsv.exceptions.CsvValidationException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ElasticHelper;
import utils.Maps;
import utils.Settings;
import utils.Utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class PowerGenAgent extends Agent {
    Settings settingsInstance = Settings.getSettingsInstance();
    private boolean done = false;
    private ACLMessage pmsg = null;
    private ACLMessage smsg = null;

    private boolean isOn = false;
    private boolean isPaused = false;
    private double maxCapacity = 0;
    private double holdCapacity = 0;
    //toAdd is sampled from a probability distribution of actual power grid rates.
    private int toAdd = 0;
    private int maxGenCapacity = 0;
    private int rateSecs = settingsInstance.getRateSecsPowerGen();
    private double agent_X = 0;
    private double agent_Y = 0;
    private ImageView agentImageView;
    private Label agentLabel;
    private double capacityFactor = settingsInstance.getInitCapacityFactor();

    private Utils utility = new Utils();
    private Power powerInstance = Power.getPowerInstance();
    private Maps mapsInstance = Maps.getMapsInstance();
    private Map.Entry<AID, Double> nearestNeighbour = null;
    private LinkedHashMap<AID, Double> nearestNeighbours = new LinkedHashMap<>();
    private ArrayList<Behaviour> behaviourList = new ArrayList<>();
    private boolean pauseAgent = false;

    //message sending flags
    private boolean sentCFP = false;
    private int countCFP = 0;
    private int countCFPX = 500;
    private AID currentNeighbour;
    private AID nextNeighbour;
    private long retryTime = 2000;

    //colours
    private int currentColour = 0;
    private int GREEN = 1;
    private int BLUE = 2;

    //logs
    private static Logger LOGGER = LoggerFactory.getLogger(PowerGenAgent.class);
    private static ElasticHelper elasticHelper = ElasticHelper.getElasticHelperInstance();

    @Override
    protected void takeDown() {
        super.takeDown();
        LOGGER.info(getLocalName() + " takedown. Killing...");
        powerInstance.subtractGridMax(maxCapacity);
        powerInstance.subtractPowerLevel(holdCapacity);
        powerInstance.subtractGenRate(toAdd);
        setGenRateLabel();
        try { DFService.deregister(this); }
        catch (Exception e) {}
        if(!behaviourList.isEmpty()) {
            for (Behaviour b: behaviourList){
                LOGGER.info("Removing behaviour(s): "+b);
                removeBehaviour(b);
            }
        }
        mapsInstance.removeUI(agentImageView);
        mapsInstance.removeUI(agentLabel);
        doDelete();
    }

    protected void setup() {
        determineCapacity();
        String message = getAID().getLocalName() + " started with capacity of " + maxCapacity + " and genrate of "
                + toAdd + "/" + rateSecs + " ms.";
        LOGGER.info(message);
        elasticHelper.indexLogs(this, message);

        Utils utils = new Utils();
        utils.registerServices(this, "Power-Generation");

        InitPosition initPosition = new InitPosition(this, 2000);
        ReceiveMessage receiveMessage = new ReceiveMessage();
        PeriodicPowerGeneration periodicPowerGeneration = new PeriodicPowerGeneration(this, rateSecs);
        CheckSimulationState checkSimulationState = new CheckSimulationState(this, settingsInstance.getSimCheckRate());
        behaviourList.add(initPosition);
        behaviourList.add(receiveMessage);
        behaviourList.add(periodicPowerGeneration);
        behaviourList.add(checkSimulationState);
        //addBehaviour(new GeneratePower());
        addBehaviour(initPosition);
        addBehaviour(receiveMessage);
        addBehaviour(periodicPowerGeneration);
        addBehaviour(checkSimulationState);
    }

    private void determineCapacity() {
        //sample storage capacity and generation from actual US energy data.
        //maxCapacity = new Random().ints(500000, 1000000).findFirst().getAsInt();
        try {
            maxCapacity = utility.getStorageCapacity();
            powerInstance.addGridMax(maxCapacity);
            maxGenCapacity = (int) (utility.getPowerGenRate()*1000);
            toAdd = (int) (maxGenCapacity*capacityFactor); //original vals are in mwh, convert to kwh;
            powerInstance.addGenRate(toAdd);
            LOGGER.debug("Randomised: " + String.valueOf(toAdd));
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
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
        setGenRateLabel();
    }

    private void setGenRateLabel() {
        HashMap<String, Label> genHM = mapsInstance.getDemandGenLabel();
        Label genRate = genHM.get("genRate");
        double genLevel = powerInstance.getGenRate();
        BigDecimal bd = new BigDecimal(genLevel).setScale(2, RoundingMode.HALF_UP);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                genRate.setText("G (kwh/s): " + bd.doubleValue());
            }
        });
    }

    private class GeneratePower extends OneShotBehaviour {
        @Override
        public void action() {
            LOGGER.info("Obtaining total system power levels...");
            Power powerInstance = Power.getPowerInstance();
            LOGGER.info("Total power levels:" + powerInstance.showPowerLevels());

            LOGGER.info("Initial power generation...");
            powerInstance.addPowerLevel(1000);
            LOGGER.info("Total power levels:" + powerInstance.showPowerLevels());

        }
    }

    private class InitPosition extends WakerBehaviour {
        public InitPosition(Agent a, long timeout) {
            super(a, timeout);
        }
        @Override
        protected void onWake() {
            super.onWake();
            initPosition();
            nearestNeighbour = utility.getNearest(this.myAgent, agent_X, agent_Y, "Power-Storage_Distribution");
            nearestNeighbours = utility.getNearestObjectsList(this.myAgent, agent_X, agent_Y, "Power-Storage_Distribution");
        }
    }

    private class PeriodicPowerGeneration extends TickerBehaviour {
        public PeriodicPowerGeneration(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            //very convoluted...
            //should rewrite at some point
            double addTo = toAdd;
            double tempHolder = holdCapacity + addTo;
            double capacity = (holdCapacity/maxCapacity) * 100;
            //LOGGER.debug("STORAGE CAPACITY: " + capacity + "%");

            if (pauseAgent == false) {
                if (pmsg != null && pmsg.getContent().equals("START")) {

                        if (holdCapacity < maxCapacity) {
                            isOn = true;
                            if (currentColour != GREEN) {
                                try {
                                    mapsInstance.changeColor(agentImageView, "GREEN");
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                currentColour = GREEN;
                            }
//                            powerInstance.subtractGenRate(toAdd);
//                            powerInstance.addGenRate(toAdd);

                            //code to pre vent power exceeding maxcapacity
                            if (tempHolder >= maxCapacity) {
                                addTo = maxCapacity - holdCapacity;
                                powerInstance.addPowerLevel(addTo);
                                holdCapacity = holdCapacity + addTo;
                            } else if (tempHolder < maxCapacity) {
                                powerInstance.addPowerLevel(toAdd);
                                holdCapacity = holdCapacity + toAdd;
                            }

                            LOGGER.info(myAgent.getLocalName() + " total power levels1: " + String.valueOf(holdCapacity));
                            if (currentColour != GREEN) {
                                try {
                                    mapsInstance.changeColor(agentImageView, "GREEN");
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                currentColour = GREEN;
                            }
                        } else if (holdCapacity >= maxCapacity) {
                            LOGGER.info("Max capacity at " + holdCapacity + " of " + getName() + " . Paused generation.");

                            if (currentColour != BLUE) {
                                try {
                                    mapsInstance.changeColor(agentImageView, "BLUE");
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                currentColour = BLUE;
                            }
                        }
                        if (capacity>=85) {
                            if (!sentCFP) {
                                currentNeighbour = nearestNeighbour.getKey();
                                utility.sendMessage(myAgent, currentNeighbour, "BEGIN_STORE", "PROPOSE");
                                sentCFP = true;
                            } else if (sentCFP) {
                                if (smsg != null) {
                                    if (smsg.getContent().equals("ACCEPT_STORE")) {
                                        //System.out.println(myAgent.getLocalName()+ " transferring to nearest neighbour: " + nearestNeighbour.getKey().getLocalName());
                                        HashMap.Entry<String, String> arguments = new HashMap.SimpleEntry<String, String>("toAdd", String.valueOf(toAdd));
                                        utility.sendMessageWithArgs(myAgent, currentNeighbour, arguments, "ADD", "REQUEST");
//                                        powerInstance.subtractGenRate(toAdd);
//                                        powerInstance.addGenRate(toAdd);

                                    } else if (smsg.getContent().equals("REJECT_STORE")) {
    //                                System.out.println(myAgent.getLocalName() + ": REJECT_STORE :" + countCFP);
                                        Iterator iterator = nearestNeighbours.keySet().iterator();
                                        while (iterator.hasNext()) {
                                            AID temp = (AID) iterator.next();
                                            //System.out.println("AGENT: " + myAgent.getLocalName() + " CURRENTNEIGHBOUR:" + currentNeighbour + " TEMP:" + temp);
                                            if (temp.getLocalName().equals(currentNeighbour.getLocalName()) && iterator.hasNext()) {
                                                nextNeighbour = (AID) iterator.next();
                                                LOGGER.debug("CURRENT:" + temp.getLocalName() + " NEXT:" + nextNeighbour.getLocalName());
                                                utility.sendMessage(myAgent, nextNeighbour, "BEGIN_STORE", "PROPOSE");
                                                currentNeighbour = nextNeighbour;
                                                break;
                                            }
                                            if (!iterator.hasNext()) {
                                                try {
                                                    Thread.sleep(retryTime);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                                currentNeighbour = temp;
                                                sentCFP = false;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                    }
                } else if (pmsg != null && pmsg.getContent().equals("STOP")) {
                    isOn = false;
                    LOGGER.info(getAID().getName() + " STOPPING POWER GENERATION...");
                    pmsg = null;
                    block();
                } else {
                    block();
                }
            } else if (pauseAgent) {
                block();
            }
        }

        @Override
        public void stop() {
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
            if (msg != null) {
                String contents = msg.getContent();
                switch (contents) {
                    case "GENRATE_QUERY":
                        ACLMessage reply_query = msg.createReply();
                        reply_query.setContent("max:" + maxGenCapacity + " current:" + toAdd + " capacity_factor:" + capacityFactor);
                        send(reply_query);
                        break;
                    case "GENRATE_INCR":
                        double to_inc = Double.valueOf(msg.getAllUserDefinedParameters().get("toAdd").toString());
                        double temp_cf_i = capacityFactor + to_inc;
                        ACLMessage reply_incr = msg.createReply();
                        if (temp_cf_i > 0.0 && temp_cf_i < 1.0) {
                            capacityFactor = temp_cf_i;
                            int og_toAdd = toAdd;
                            powerInstance.subtractGenRate(og_toAdd);
                            toAdd = (int) (maxGenCapacity*capacityFactor);
                            powerInstance.addGenRate(toAdd);
                            reply_incr.setContent("INCR_ACCEPTED");
                            send(reply_incr);
                            LOGGER.info("INCR_ACCEPTED");
                        } else {
                            reply_incr.setContent("INCR_REJECTED");
                            send(reply_incr);
                            LOGGER.info("INCR_REJECTED");
                        }
                        break;
                    case "GENRATE_DECR":
                        double to_dec = Double.valueOf(msg.getAllUserDefinedParameters().get("toAdd").toString());
                        double temp_cf_d = capacityFactor - to_dec;
                        ACLMessage reply_decr = msg.createReply();
                        if (temp_cf_d > 0.0 && temp_cf_d < 1.0) {
                            capacityFactor = temp_cf_d;
                            int og_toDec = toAdd;
                            powerInstance.subtractGenRate(og_toDec);
                            toAdd = (int) (maxGenCapacity*capacityFactor);
                            powerInstance.addGenRate(toAdd);
                            reply_decr.setContent("DECR_ACCEPTED");
                            send(reply_decr);
                            LOGGER.info("DECR_ACCEPTED");
                        } else {
                            reply_decr.setContent("DECR_REJECTED");
                            send(reply_decr);
                            LOGGER.info("DECR_REJECTED");
                        }
                        break;
                    case "PAUSE":
                        pauseAgent = true;
                        break;
                    case "RESUME" :
                        pauseAgent = false;
                        break;
                    case "START":
                        pmsg = msg;
                        break;
                    case "ACCEPT_STORE":
                    case "REJECT_STORE":
                        smsg = msg;
                        break;
                    case "BEGIN_CONSUME":
                        ACLMessage reply = msg.createReply();
                        double toConsume = Double.parseDouble(msg.getAllUserDefinedParameters().entrySet().iterator().next().getValue().toString());
                        if (holdCapacity > 0 && holdCapacity >= toConsume) {
                            reply.setPerformative(ACLMessage.AGREE);
                            reply.setContent("ACCEPT_CONSUME");
                            holdCapacity = holdCapacity - toConsume;
                            powerInstance.subtractPowerLevel(toConsume);
                            if (currentColour == BLUE) {
                                try {
                                    mapsInstance.changeColor(agentImageView, "GREEN");
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                currentColour = GREEN;
                            }
                            LOGGER.info(myAgent.getLocalName() + " transferred " + toConsume + " to " + msg.getSender().getLocalName()
                                    + ". Current power levels:" + String.valueOf(holdCapacity));
                            send(reply);
                        } else if (holdCapacity <= 0 || holdCapacity < toConsume) {
                            reply.setPerformative(ACLMessage.AGREE);
                            //System.out.println(myAgent.getLocalName()+" rejected " + msg.getSender().getLocalName());
                            reply.setContent("REJECT_CONSUME");
                            send(reply);
                        }
                        break;
                    case "KILL":
                        takeDown();
                        break;
                }
            }
            else {
                block();
            }
        }
    }

}