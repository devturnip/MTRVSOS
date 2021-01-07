package power;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Random;

public class PowerGenAgent extends Agent {
    private boolean done = false;
    private ACLMessage pmsg = null;

    private boolean isOn = false;
    private boolean isPaused = false;
    private double maxCapacity = 0;
    private double holdCapacity = 0;
    private int toAdd = new Random().ints(1000, 10000).findFirst().getAsInt();
    private int rateSecs = 2000;

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

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Power-Generation");
        sd.setName(getLocalName()+"Power-Generation");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        //addBehaviour(new GeneratePower());
        addBehaviour(new ReceiveMessage());
        addBehaviour(new PeriodicPowerGeneration(this, rateSecs));
    }

    private void determineCapacity() {
        maxCapacity = new Random().ints(500000, 1000000).findFirst().getAsInt();
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

    private class PeriodicPowerGeneration extends TickerBehaviour {
        public PeriodicPowerGeneration(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (pmsg != null && pmsg.getContent().equals("START")) {
                if (isPaused == false) {
                    if (holdCapacity < maxCapacity) {
                        isOn = true;
                        Power powerInstance = Power.getPowerInstance();
                        powerInstance.addPowerLevel(toAdd);
                        holdCapacity = holdCapacity + toAdd;
                        System.out.println("Total power levels:" + powerInstance.showPowerLevels());
                    } else if (holdCapacity >= maxCapacity) {
                        maxCapacity = holdCapacity;
                        System.out.println("Max capacity at " + maxCapacity + " of " + getName() + " . Paused generation.");
                        isPaused = true;
                    }
                } else if (isPaused == true && holdCapacity < maxCapacity) {
                    isPaused = false;
                    Power powerInstance = Power.getPowerInstance();
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