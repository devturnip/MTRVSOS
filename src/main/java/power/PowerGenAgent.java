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

    @Override
    protected void takeDown() {
        super.takeDown();
        try { DFService.deregister(this); }
        catch (Exception e) {}
    }

    protected void setup() {
        //System.out.println("Hi, I'm Agent " + getAID().getLocalName());
        System.out.println("My GUID is " + getAID().getName());
        //System.out.println("My addresses are " + String.join(",", getAID().getAddressesArray()));

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

        addBehaviour(new GeneratePower());
        addBehaviour(new ReceiveMessage());
        addBehaviour(new PeriodicPowerGeneration(this, 2000));
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
                Power powerInstance = Power.getPowerInstance();
                powerInstance.addPowerLevel(new Random().ints(500, 10000).findFirst().getAsInt());
                System.out.println("Total power levels:" + powerInstance.showPowerLevels());
            }
            else if (pmsg != null && pmsg.getContent().equals("STOP")){
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