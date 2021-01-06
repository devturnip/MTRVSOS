package SoS;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import utils.Utils;

public class SoSAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println("My GUID is " + getAID().getName());
        addBehaviour(new ContactPower(this,10000));
    }

    private class ContactPower extends WakerBehaviour {
        public ContactPower(Agent a, long timeout) {
            super(a, timeout);
        }
        @Override
        protected void onWake() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            Utils util = new Utils();
            AID[] agents = util.getAgentNamesByService(SoSAgent.this, "Power-Generation");
            for (int i=0; i<agents.length; i++) {
                System.out.println(agents[i]);
            }
        }
    }
}
