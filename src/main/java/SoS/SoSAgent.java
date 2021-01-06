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
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("Power-Generation");
            dfd.addServices(sd);
            SearchConstraints ALL = new SearchConstraints();
            ALL.setMaxResults(new Long(-1));
            try {
                DFAgentDescription[] result = DFService.search(SoSAgent.this, dfd, ALL);
                AID[] agents = new AID[result.length];
                for (int i=0; i<result.length; i++) {
                    agents[i] = result[i].getName();
                    System.out.println(agents[i]);
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }
}
