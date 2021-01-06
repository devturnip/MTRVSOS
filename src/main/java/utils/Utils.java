package utils;

import SoS.SoSAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class Utils {
    public Utils() {
    }
    public AID[] getAgentNamesByService(Agent a, String service) {
        AID[] agents = null;
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(service);
        dfd.addServices(sd);
        SearchConstraints ALL = new SearchConstraints();
        ALL.setMaxResults(new Long(-1));
        try {
            DFAgentDescription[] result = DFService.search(a, dfd, ALL);
            agents = new AID[result.length];
            for (int i=0; i<result.length; i++) {
                agents[i] = result[i].getName();
            }
            return agents;
        } catch (FIPAException fe) {
            fe.printStackTrace();
            return null;
        }
    }
}
