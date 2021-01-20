package power;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import javafx.scene.image.ImageView;
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

    @Override
    protected void takeDown() {
        super.takeDown();
        try { DFService.deregister(this); }
        catch (Exception e) {}
    }

    protected void setup() {
        //System.out.println("Hi, I'm Agent " + getAID().getLocalName());
        //System.out.println("My addresses are " + String.join(",", getAID().getAddressesArray()));

        determineCapacity();
        System.out.println(getAID().getName() + " started with capacity of " + maxCapacity + " and genrate of "
         + toAdd + "/" + rateSecs + " ms.");

        Utils utils = new Utils();
        utils.registerServices(this, "Power-Generation");

        //addBehaviour(new GeneratePower());
        addBehaviour(new InitPosition(this, 2000));
        addBehaviour(new ReceiveMessage());
        addBehaviour(new PeriodicPowerGeneration(this, rateSecs));
    }

    private void determineCapacity() {
        //similarly, capacity needs to be sampled from an actual distribution of capacities.
        maxCapacity = new Random().ints(500000, 1000000).findFirst().getAsInt();
    }

    private void initPosition() {
        HashMap<String, ImageView> hm = mapsInstance.getAgentMap(this.getLocalName(), true);
        HashMap.Entry<String, ImageView> entry = hm.entrySet().iterator().next();
        String agentName = entry.getKey();
        ImageView iv = entry.getValue();
        agentImageView = iv;
        agent_X = iv.getX();
        agent_Y = iv.getY();
        System.out.println("THIS:" + this.getLocalName() + " agent:" + agentName + " X:" + agent_X + " Y:" + agent_Y);

    }

    private class GeneratePower extends OneShotBehaviour {
        @Override
        public void action() {
            System.out.println("Obtaining total system power levels...");
            Power powerInstance = Power.getPowerInstance();
            System.out.println("Total power levels:" + powerInstance.showPowerLevels());

            System.out.println("Initial power generation...");
            powerInstance.addPowerLevel(1000);
            System.out.println("Total power levels:" + powerInstance.showPowerLevels());

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
            if (pmsg != null && pmsg.getContent().equals("START")) {
                if (!isPaused) {
                    if (holdCapacity < maxCapacity) {
                        isOn = true;
                        powerInstance.addPowerLevel(toAdd);
                        holdCapacity = holdCapacity + toAdd;
                        System.out.println(myAgent.getLocalName() + " total power levels: " + String.valueOf(holdCapacity));
                        if (currentColour != GREEN) {
                            try {
                                mapsInstance.changeColor(agentImageView, "GREEN");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            currentColour = GREEN;
                        }
                    } else if (holdCapacity >= maxCapacity) {
                        maxCapacity = holdCapacity;
                        System.out.println("Max capacity at " + maxCapacity + " of " + getName() + " . Paused generation.");
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
                    powerInstance.addPowerLevel(toAdd);
                    holdCapacity = holdCapacity + toAdd;
                    System.out.println(myAgent.getLocalName() + " total power levels: " + String.valueOf(holdCapacity));
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
                                        System.out.println("CURRENT:" + temp.getLocalName() + " NEXT:" + nextNeighbour.getLocalName());
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
                                /*if (countCFP >= countCFPX) {
                                    sentCFP = false;
                                    countCFP = 0;
                                }
                                countCFP += 1;*/
                            }
                        }
                    }
                }
            }
            else if (pmsg != null && pmsg.getContent().equals("STOP")){
                isOn = false;
                System.out.println(getAID().getName() + " STOPPING POWER GENERATION...");
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
                            System.out.println(myAgent.getLocalName() + " transferred " + toConsume + " to " + msg.getSender().getLocalName()
                                    + ". Current power levels:" + String.valueOf(holdCapacity));
                            send(reply);
                        } else if (holdCapacity <= 0 || holdCapacity < toConsume) {
                            reply.setPerformative(ACLMessage.AGREE);
                            //System.out.println(myAgent.getLocalName()+" rejected " + msg.getSender().getLocalName());
                            reply.setContent("REJECT_CONSUME");
                            send(reply);
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