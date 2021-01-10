package utils;

import SoS.SoSAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import javafx.scene.image.ImageView;

import java.util.HashMap;

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

    public void registerServices(Agent a, String serviceType) {
        String fullServiceName = a.getLocalName() + serviceType;
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(a.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        sd.setName(fullServiceName);
        dfd.addServices(sd);
        try {
            DFService.register(a, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

    }
}
