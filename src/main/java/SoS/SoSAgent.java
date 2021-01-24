package SoS;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import power.PowerGenAgent;
import utils.Utils;

public class SoSAgent extends Agent {
    //logs
    private static Logger LOGGER = LoggerFactory.getLogger(SoSAgent.class);

    @Override
    protected void setup() {
        LOGGER.info(getLocalName() + " started.");
        addBehaviour(new ContactPower(this,10000, "START"));
        //addBehaviour(new ContactPower(this,20000, "STOP"));
    }

    private class ContactPower extends WakerBehaviour {
        private String message;
        public ContactPower(Agent a, long timeout, String message) {
            super(a, timeout);
            this.message = message;
        }

        @Override
        protected void onWake() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

            Utils util = new Utils();
            AID[] agents = util.getAgentNamesByService(SoSAgent.this, "Power-Generation");
            for (int i=0; i<agents.length; i++) {
                //System.out.println(agents[i]);
                if (agents[i] != null) {
                    msg.addReceiver(agents[i]);
                    msg.setContent(message);
                    send(msg);
                    LOGGER.info(this.myAgent.getName()+ " sent (" + message + ") to " + agents[i].getName());
                }
            }
        }
    }
}
