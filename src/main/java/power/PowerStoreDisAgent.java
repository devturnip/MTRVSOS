package power;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import utils.ElasticHelper;
import utils.Maps;
import utils.Settings;
import utils.Utils;

import java.util.*;

public class PowerStoreDisAgent extends Agent {
    private double maxCapacity = 0;
    private double holdCapacity = 0;
    private double agent_X = 0;
    private double agent_Y = 0;
    private ImageView agentImageView;
    private Label agentLabel;
    private ArrayList<Behaviour> behaviourList = new ArrayList<>();
    private AID correspondent;
    private boolean pauseAgent = false;
    private Settings settingsInstance = Settings.getSettingsInstance();

    private Maps mapsInstance = Maps.getMapsInstance();
    private Power powerInstance = Power.getPowerInstance();
    private Utils utils = new Utils();

    //message sending flags
    private int countCFP = 0;

    //colours
    private int currentColour = 0;
    private int GREEN = 1;
    private int BLUE = 2;
    private int ORANGE = 3;

    //logs
    private static Logger LOGGER = LoggerFactory.getLogger(PowerStoreDisAgent.class);
    private static ElasticHelper elasticHelper = ElasticHelper.getElasticHelperInstance();

    @Override
    protected void setup() {
        super.setup();
        determineCapacity();

        LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
        logArgs.put("action", "power_distribution.init");
        logArgs.put("max_capacity", String.valueOf(maxCapacity));

        String message = getAID().getLocalName() + " started with capacity of " + maxCapacity;
        LOGGER.info(message);
        elasticHelper.indexLogs(this, logArgs);

        Utils utils = new Utils();

        String[] arguments = {"Power-Storage_Distribution","EV-Charging"};
        utils.registerServices(this, arguments);

        InitPosition initPosition = new InitPosition(this, settingsInstance.getMSToWait());
        ReceiveMessage receiveMessage = new ReceiveMessage();
        CheckSimulationState checkSimulationState = new CheckSimulationState(this, settingsInstance.getSimCheckRate());
        behaviourList.add(initPosition);
        behaviourList.add(receiveMessage);
        behaviourList.add(checkSimulationState);
        addBehaviour(initPosition);
        addBehaviour(receiveMessage);
        addBehaviour(checkSimulationState);
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        String message = getLocalName() + " takedown. Killing...";
        LOGGER.info(message);
        elasticHelper.indexLogs(this, message);
        powerInstance.subtractGridMax(maxCapacity);
        powerInstance.subtractPowerLevel(holdCapacity);
        try { DFService.deregister(this); }
        catch (Exception e) {}
        if(!behaviourList.isEmpty()) {
            String log = "";
            for (Behaviour b: behaviourList){
                log = getLocalName() + " Removing behaviour(s): "+ b.getBehaviourName();
                LOGGER.info(log);
                elasticHelper.indexLogs(this, log);
                removeBehaviour(b);
            }
        }
        if (correspondent!=null){
            utils.sendMessage(this, correspondent, "REJECT_STORE", "REFUSE");
        }
        mapsInstance.removeUI(agentImageView);
        mapsInstance.removeUI(agentLabel);
        doDelete();
    }

    private void determineCapacity() {
        maxCapacity = new Random().ints(100000, 500000).findFirst().getAsInt();
        powerInstance.addGridMax(maxCapacity);
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
        }
    }

    private class GeneratePower extends OneShotBehaviour {
        private String toAdd = "";
        public GeneratePower(String toAdd) {
            super();
            this.toAdd = toAdd;
        }
        @Override
        public void action() {
            if (pauseAgent == false) {
                if (holdCapacity < maxCapacity) {
                    double addTo = Double.parseDouble(toAdd);
                    double temp = holdCapacity + addTo;
                    if (temp >= maxCapacity) {
                        addTo = maxCapacity - holdCapacity;
                        powerInstance.addPowerLevel(addTo);
                        holdCapacity = holdCapacity + addTo;
                    } else {
                        powerInstance.addPowerLevel(addTo);
                        holdCapacity = holdCapacity + addTo;
                    }
                    String log = getLocalName() + " total power levels: " + holdCapacity;
                    LOGGER.info(log);

                    if (currentColour != GREEN) {
                        try {
                            mapsInstance.changeColor(agentImageView, "GREEN");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        currentColour = GREEN;
                    }
                } else if (holdCapacity >= maxCapacity) {
                    LOGGER.info("Max capacity at " + maxCapacity + " of " + getName() + " . Paused.");
                    if (currentColour != BLUE) {
                        try {
                            mapsInstance.changeColor(agentImageView, "BLUE");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        currentColour = BLUE;
                    }
                }
            } else if (pauseAgent) {
                block();
            }
        }
    }

    private class CheckSimulationState extends TickerBehaviour {
        public CheckSimulationState(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            pauseAgent = settingsInstance.getSimulationState();
            if ((holdCapacity/maxCapacity * 100.0) <= 5.0 && agentImageView!= null) {
                if (currentColour != ORANGE) {
                    try {
                        mapsInstance.changeColor(agentImageView, "ORANGE");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    currentColour = ORANGE;
                }
            }
        }
    }

    private class ReceiveMessage extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();

            if (msg != null) {
                String contents = "";
                int performative = msg.getPerformative();
                switch (performative) {
                    case 11:
                        contents = msg.getContent();
                        if (contents.equals("BEGIN_STORE")) {
                            LOGGER.info(myAgent.getLocalName() + " received PROPOSE (" + contents + ") from " + msg.getSender().getLocalName());
                            ACLMessage reply = msg.createReply();
                            boolean send = true;
                            if (holdCapacity <= 0 || holdCapacity < maxCapacity) {
                                if (holdCapacity <= 0) {
                                    correspondent = msg.getSender();
                                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    reply.setContent("ACCEPT_STORE");
                                }
                                else {
                                    double threshold = (holdCapacity / maxCapacity) * 100;
                                    if (threshold >= 65) {
                                        //if capacity >= val, randomise chance of rejecting to allow other storage agents
                                        //to receive power.
                                        LOGGER.debug(getLocalName() + " threshold:" + threshold);
                                        send = new Random().nextBoolean();
                                    }
                                    correspondent = msg.getSender();
                                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    if (send) {
                                        reply.setContent("ACCEPT_STORE");
                                    } else {
                                        reply.setContent("REJECT_STORE");
                                    }
                                }
                                LOGGER.info("SENDING REPLY(" + reply.getContent()+") to " + msg.getSender().getLocalName());
                                LOGGER.debug("HOLD:" + holdCapacity + " MAX:" + maxCapacity);
                                send(reply);

                            } else if (holdCapacity >= maxCapacity) {
                                correspondent = null;
                                myAgent.addBehaviour(new GeneratePower("0"));
                                reply.setPerformative(ACLMessage.REFUSE);
                                reply.setContent("REJECT_STORE");
                                send(reply);
                            }
                        }
                        break;
                    case 16:
                        contents = msg.getContent();
                        ACLMessage reply = msg.createReply();
                        switch(contents) {
                            case "PAUSE":
                                pauseAgent = true;
                                break;
                            case "RESUME" :
                                pauseAgent = false;
                                break;
                            case "ADD":
                                String key = (String)msg.getAllUserDefinedParameters().entrySet().iterator().next().getKey();
                                String value = (String)msg.getAllUserDefinedParameters().entrySet().iterator().next().getValue();
                                if(holdCapacity == 0 || holdCapacity < maxCapacity) {
                                    myAgent.addBehaviour(new GeneratePower(value));

                                    LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
                                    logArgs.put("action", "power_distribution.power_receive");
                                    logArgs.put("accept_receive", "true");
                                    logArgs.put("receive_amount", String.valueOf(value));
                                    logArgs.put("received_from", msg.getSender().getLocalName());

                                    elasticHelper.indexLogs(myAgent, logArgs);

                                } else if (holdCapacity >= maxCapacity) {
                                    myAgent.addBehaviour(new GeneratePower(value));
                                    ACLMessage replyBack = msg.createReply();
                                    replyBack.setPerformative(ACLMessage.REFUSE);
                                    replyBack.setContent("REJECT_STORE");
                                    send(replyBack);

                                    LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
                                    logArgs.put("action", "power_distribution.power_receive");
                                    logArgs.put("accept_receive", "false");

                                    elasticHelper.indexLogs(myAgent, logArgs);


                                }
                                break;
                            case "BEGIN_CONSUME":
//                                reply = msg.createReply();
                                double toConsume = Double.parseDouble(msg.getAllUserDefinedParameters().entrySet().iterator().next().getValue().toString());
                                if (holdCapacity >= 0 && holdCapacity >= toConsume) {
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
                                    + ". Current power levels:" + holdCapacity);
                                    send(reply);

                                    LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
                                    logArgs.put("action", "power_distribution.power_transfer");
                                    logArgs.put("accept_transfer", "true");
                                    logArgs.put("transfer_amount", String.valueOf(toConsume));
                                    logArgs.put("transfer_to", msg.getSender().getLocalName());
                                    logArgs.put("current_capacity", String.valueOf(holdCapacity));
                                    elasticHelper.indexLogs(myAgent, logArgs);

                                } else if (holdCapacity <= 0 || holdCapacity < toConsume) {
                                    reply.setPerformative(ACLMessage.AGREE);
                                    reply.setContent("REJECT_CONSUME");
                                    send(reply);

                                    LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
                                    logArgs.put("action", "power_distribution.power_transfer");
                                    logArgs.put("accept_transfer", "false");
                                    elasticHelper.indexLogs(myAgent, logArgs);

                                }
                                break;
                            case "BEGIN_CHARGE":
//                                reply = msg.createReply();
                                LOGGER.debug(getLocalName()+" received charge request from: "+msg.getSender().getLocalName());
                                double toCharge = Double.parseDouble(msg.getAllUserDefinedParameters().entrySet().iterator().next().getValue().toString());
                                if (holdCapacity > 0 && holdCapacity >= toCharge) {
                                    reply.setPerformative(ACLMessage.AGREE);
                                    reply.setContent("ACCEPT_CHARGE");
                                    holdCapacity = holdCapacity - toCharge;
                                    powerInstance.subtractPowerLevel(toCharge);
                                    if (currentColour == BLUE) {
                                        try {
                                            mapsInstance.changeColor(agentImageView, "GREEN");
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        currentColour = GREEN;
                                    }
                                    LOGGER.info(myAgent.getLocalName() + " transferred " + toCharge + " to " + msg.getSender().getLocalName()
                                            + ". Current power levels:" + holdCapacity);
                                    send(reply);

                                    LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
                                    logArgs.put("action", "power_distribution.power_transfer");
                                    logArgs.put("accept_transfer", "true");
                                    logArgs.put("transfer_amount", String.valueOf(toCharge));
                                    logArgs.put("transfer_to", msg.getSender().getLocalName());
                                    logArgs.put("current_capacity", String.valueOf(holdCapacity));
                                    elasticHelper.indexLogs(myAgent, logArgs);

                                } else if (holdCapacity <= 0 || holdCapacity < toCharge) {
                                    reply.setPerformative(ACLMessage.AGREE);
                                    reply.setContent("REJECT_CONSUME");
                                    send(reply);

                                    LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
                                    logArgs.put("action", "power_distribution.power_transfer");
                                    logArgs.put("accept_transfer", "false");
                                    elasticHelper.indexLogs(myAgent, logArgs);
                                }
                                break;

                        }
                }
            } else {
                block();
            }
        }
    }
}
