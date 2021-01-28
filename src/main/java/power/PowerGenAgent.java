package power;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Maps;
import utils.Utils;

import java.util.*;

public class PowerGenAgent extends Agent {
    private boolean done = false;
    private ACLMessage pmsg = null;
    private ACLMessage smsg = null;

    private boolean isOn = false;
    private boolean isPaused = false;
    private double maxCapacity = 0;
    private double holdCapacity = 0;
    //toAdd needs to be sampled from a probability distribution of actual power grid rates.
    private int toAdd = new Random().ints(1000, 10000).findFirst().getAsInt();
    private int rateSecs = 200;
    private double agent_X = 0;
    private double agent_Y = 0;
    private ImageView agentImageView;

    private Utils utility = new Utils();
    private Power powerInstance = Power.getPowerInstance();
    private Maps mapsInstance = Maps.getMapsInstance();
    private Map.Entry<AID, Double> nearestNeighbour = null;
    private LinkedHashMap<AID, Double> nearestNeighbours = new LinkedHashMap<>();
    private ArrayList<Behaviour> behaviourList = new ArrayList<>();

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

    @Override
    protected void takeDown() {
        super.takeDown();
        LOGGER.info(getLocalName() + " takedown. Killing...");
        powerInstance.subtractGridMax(maxCapacity);
        try { DFService.deregister(this); }
        catch (Exception e) {}
        if(!behaviourList.isEmpty()) {
            for (Behaviour b: behaviourList){
                LOGGER.info("Removing behaviour(s): "+b);
                removeBehaviour(b);
            }
        }
        mapsInstance.removeUI(agentImageView);
        doDelete();
    }

    protected void setup() {
        determineCapacity();
        LOGGER.info(getAID().getLocalName() + " started with capacity of " + maxCapacity + " and genrate of "
         + toAdd + "/" + rateSecs + " ms.");

        Utils utils = new Utils();
        utils.registerServices(this, "Power-Generation");

        InitPosition initPosition = new InitPosition(this, 2000);
        ReceiveMessage receiveMessage = new ReceiveMessage();
        PeriodicPowerGeneration periodicPowerGeneration = new PeriodicPowerGeneration(this, rateSecs);
        behaviourList.add(initPosition);
        behaviourList.add(receiveMessage);
        behaviourList.add(periodicPowerGeneration);
        //addBehaviour(new GeneratePower());
        addBehaviour(initPosition);
        addBehaviour(receiveMessage);
        addBehaviour(periodicPowerGeneration);
    }

    private void determineCapacity() {
        //similarly, capacity needs to be sampled from an actual distribution of capacities.
        maxCapacity = new Random().ints(500000, 1000000).findFirst().getAsInt();
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
//            Iterator iterator = nearestNeighbours.entrySet().iterator();
//            while(iterator.hasNext()) {
//                System.out.println("INIT SET:" + iterator.next());
//            }
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

            if (pmsg != null && pmsg.getContent().equals("START")) {
                if (!isPaused) {
                    if (holdCapacity < maxCapacity) {
                        isOn = true;

                        //code to prevent power exceeding maxcapacity
                        if (tempHolder >= maxCapacity) {
                            addTo = tempHolder - maxCapacity;
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
                        LOGGER.info("Max capacity at " + maxCapacity + " of " + getName() + " . Paused generation.");
                        isPaused = true;
                        if (currentColour != BLUE) {
                            try {
                                mapsInstance.changeColor(agentImageView, "BLUE");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            currentColour = BLUE;
                        }
                    }
                } else if (isPaused && holdCapacity < maxCapacity) {
                    isPaused = false;

                    //code to prevent power exceeding maxcapacity
                    if (tempHolder > maxCapacity) {
                        addTo = tempHolder - maxCapacity;
                        powerInstance.addPowerLevel(addTo);
                        holdCapacity = holdCapacity + addTo;
                    } else {
                        powerInstance.addPowerLevel(addTo);
                        holdCapacity = holdCapacity + addTo;
                    }
                    if (holdCapacity >= maxCapacity) {
                        isPaused = true;
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
                } else if (isPaused && holdCapacity >= maxCapacity) {
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
            }
            else if (pmsg != null && pmsg.getContent().equals("STOP")){
                isOn = false;
                LOGGER.info(getAID().getName() + " STOPPING POWER GENERATION...");
                pmsg = null;
                block();
            }
            else {
                block();
            }
        }

        @Override
        public void stop() {
        }
    }

    private class ReceiveMessage extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                String contents = msg.getContent();
                switch (contents) {
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