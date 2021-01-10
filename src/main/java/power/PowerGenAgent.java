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
import java.util.Random;

public class PowerGenAgent extends Agent {
    private boolean done = false;
    private ACLMessage pmsg = null;

    private boolean isOn = false;
    private boolean isPaused = false;
    private double maxCapacity = 0;
    private double holdCapacity = 0;
    private int toAdd = new Random().ints(1000, 10000).findFirst().getAsInt();
    private int rateSecs = 200;

    private Utils utility = new Utils();
    private Power powerInstance = Power.getPowerInstance();
    private Maps mapsInstance = Maps.getMapsInstance();

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
        //System.out.println("LOCALNAME: " + this.getLocalName());
        //System.out.println("NAME:" + this.getName());
        HashMap<String, ImageView> hm = mapsInstance.getAgentMap(this.getLocalName(), true);
        System.out.println(hm);
//        for (String an:hm.keySet()) {
//            ImageView iv = hm.get(an);
//            System.out.println(an + ": " + iv.getX() + "," + iv.getY());
//        }
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
        }
    }

    private class PeriodicPowerGeneration extends TickerBehaviour {
        public PeriodicPowerGeneration(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
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

                        /*AID[] agents = utility.getAgentNamesByService(PowerGenAgent.this, "Power-Storage_Distribution");
                        for (int i=0; i<agents.length; i++) {
                            System.out.println(agents[i]);
                            //System.out.println("AGENT LIST");

                        }*/
                    }
                } else if (isPaused && holdCapacity < maxCapacity) {
                    isPaused = false;
                    powerInstance.addPowerLevel(toAdd);
                    holdCapacity = holdCapacity + toAdd;
                    System.out.println("Total power levels:" + powerInstance.showPowerLevels());
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
                pmsg = msg;
            }
        }
    }

}