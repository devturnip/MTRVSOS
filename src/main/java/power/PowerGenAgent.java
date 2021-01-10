package power;

import SoS.SoSAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import utils.Maps;
import utils.Utils;

import javafx.scene.image.ImageView;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PowerGenAgent extends Agent {
    private boolean done = false;
    private ACLMessage pmsg = null;
    private ACLMessage smsg = null;

    private boolean isOn = false;
    private boolean isPaused = false;
    private double maxCapacity = 0;
    private double holdCapacity = 0;
    private int toAdd = new Random().ints(1000, 10000).findFirst().getAsInt();
    private int rateSecs = 200;
    private double agent_X = 0;
    private double agent_Y = 0;

    private Utils utility = new Utils();
    private Power powerInstance = Power.getPowerInstance();
    private Maps mapsInstance = Maps.getMapsInstance();
    private Map.Entry<AID, Double> nearestNeighbour = null;

    //message sending flags
    private boolean sentCFP = false;
    private int countCFP = 0;

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
        maxCapacity = new Random().ints(500000, 1000000).findFirst().getAsInt();
    }

    private void initPosition() {
        HashMap<String, ImageView> hm = mapsInstance.getAgentMap(this.getLocalName(), true);
        HashMap.Entry<String, ImageView> entry = hm.entrySet().iterator().next();
        String agentName = entry.getKey();
        ImageView iv = entry.getValue();
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
                        System.out.println("Total power levels:" + powerInstance.showPowerLevels());
                    } else if (holdCapacity >= maxCapacity) {
                        maxCapacity = holdCapacity;
                        System.out.println("Max capacity at " + maxCapacity + " of " + getName() + " . Paused generation.");
                        isPaused = true;
                    }
                } else if (isPaused && holdCapacity < maxCapacity) {
                    isPaused = false;
                    powerInstance.addPowerLevel(toAdd);
                    holdCapacity = holdCapacity + toAdd;
                    System.out.println("Total power levels:" + powerInstance.showPowerLevels());
                } else if (isPaused && holdCapacity >= maxCapacity) {
                    if (!sentCFP) {
                        utility.sendMessage(myAgent, nearestNeighbour.getKey(), "BEGIN_STORE", "PROPOSE");
                        sentCFP = true;
                    } else if (sentCFP) {
                        if (smsg != null) {
                            if (smsg.getContent().equals("ACCEPT_STORE")) {
                                System.out.println("Transferring to nearest neighbour...");

                            } else if (smsg.getContent().equals("REJECT_STORE")) {
                                //increment to 20 before resending a new proposal
                                //ideally getNearestNeighbour should return a list of nearest neighbours
                                //in descending order, such that it would transfer to the next nearest when current nearest
                                //is full.
                                if (countCFP == 20) {
                                    sentCFP = false;
                                }
                                countCFP += 1;
                            }
                        } else {
                            if (countCFP == 20) {
                                sentCFP = false;
                            }
                            countCFP += 1;
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
                }

            }
            else {
                block();
            }
        }
    }

}