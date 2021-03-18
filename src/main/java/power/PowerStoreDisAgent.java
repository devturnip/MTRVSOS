package power;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Maps;
import utils.Settings;
import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

    //logs
    private static Logger LOGGER = LoggerFactory.getLogger(PowerStoreDisAgent.class);

    @Override
    protected void setup() {
        super.setup();
        determineCapacity();
        LOGGER.info(getLocalName() + " started with capacity of " + maxCapacity);

        Utils utils = new Utils();

//        boolean isChargingStation = new Random().nextBoolean();
//
//        if (isChargingStation) {
//            String[] arguments = {"Power-Storage_Distribution","EV-Charging"};
//            utils.registerServices(this, arguments);
//        }
//        else {
//            utils.registerServices(this, "Power-Storage_Distribution");
//        }

        String[] arguments = {"Power-Storage_Distribution","EV-Charging"};
        utils.registerServices(this, arguments);

        InitPosition initPosition = new InitPosition(this, 2000);
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
        LOGGER.info(getLocalName() + " takedown. Killing...");
        powerInstance.subtractGridMax(maxCapacity);
        powerInstance.subtractPowerLevel(holdCapacity);
        try { DFService.deregister(this); }
        catch (Exception e) {}
        if(!behaviourList.isEmpty()) {
            for (Behaviour b: behaviourList){
                LOGGER.info("Removing behaviour(s): "+b);
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
                    LOGGER.info(myAgent.getLocalName() + " total power levels: " + String.valueOf(holdCapacity));
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
                            if (holdCapacity == 0 || holdCapacity < maxCapacity) {
                                correspondent = msg.getSender();
                                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                reply.setContent("ACCEPT_STORE");
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
                                } else if (holdCapacity >= maxCapacity) {
                                    myAgent.addBehaviour(new GeneratePower(value));
                                    ACLMessage replyBack = msg.createReply();
                                    replyBack.setPerformative(ACLMessage.REFUSE);
                                    replyBack.setContent("REJECT_STORE");
                                    send(replyBack);

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
                                    + ". Current power levels:" + String.valueOf(holdCapacity));
                                    send(reply);
                                } else if (holdCapacity <= 0 || holdCapacity < toConsume) {
                                    reply.setPerformative(ACLMessage.AGREE);
                                    reply.setContent("REJECT_CONSUME");
                                    send(reply);
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
                                            + ". Current power levels:" + String.valueOf(holdCapacity));
                                    send(reply);
                                } else if (holdCapacity <= 0 || holdCapacity < toCharge) {
                                    reply.setPerformative(ACLMessage.AGREE);
                                    reply.setContent("REJECT_CONSUME");
                                    send(reply);
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
