package power;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import utils.Utils;

import java.util.Random;

public class PowerStoreDisAgent extends Agent {
    private double maxCapacity = 0;
    private double holdCapacity = 0;

    @Override
    protected void setup() {
        super.setup();
        determineCapacity();
        System.out.println(getAID().getName() + " started with capacity of " + maxCapacity);

        Utils utils = new Utils();
        utils.registerServices(this, "Power-Storage_Distribution");
    }

    private void determineCapacity() {
        maxCapacity = new Random().ints(100000, 500000).findFirst().getAsInt();
    }

    private class ReceiveMessage extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                //pmsg = msg;
            }
        }
    }
}
