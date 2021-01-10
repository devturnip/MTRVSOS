package utils;

import SoS.SoSAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

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

    public HashMap.Entry<AID, Double> getNearest(Agent agent1, double ax, double ay, String serviceName) {
        double tempDistance = 0;
        double retDistance = 0;
        AID retAgent = null;

        Maps mapInstance = Maps.getMapsInstance();
        Point2D agent1_loc = new Point2D(ax, ay);
        Point2D agent2_loc;
        AID[] agents = getAgentNamesByService(agent1, serviceName);

        for (int i=0; i<agents.length; i++) {
            HashMap.Entry<String, ImageView> entry = mapInstance.getAgentMap(agents[i].getLocalName(), true).entrySet().iterator().next();
            agent2_loc = new Point2D(entry.getValue().getX(), entry.getValue().getY());
            tempDistance = agent1_loc.distance(agent2_loc);
            //System.out.println("Distance between " + agent1.getLocalName() + " and " +
                    //agents[i].getLocalName() + " is " + tempDistance);
            if (retDistance == 0) {
                retDistance = tempDistance;
                retAgent = agents[i];
            } else if (tempDistance < retDistance) {
                retDistance = tempDistance;
                retAgent = agents[i];
            }
        }

        System.out.println("SHORTEST Distance between " + agent1.getLocalName() + " and " +
                retAgent.getLocalName() + " is " + retDistance);

        HashMap.Entry<AID, Double> retMap = new HashMap.SimpleEntry<AID, Double>(retAgent, Double.valueOf(retDistance));
        return retMap;
    }

    public void sendMessage (Agent sender, AID recipient, String message, String ACLTYPE) {
        ACLMessage msg = null;
        switch (ACLTYPE) {
            case "CFP":
                msg = new ACLMessage(ACLMessage.CFP);
                msg.addReceiver(recipient);
                msg.setContent(message);
                sender.send(msg);
                System.out.println(sender.getName()+ " sent (" + message + ") to " + recipient.getName());
                break;
            case "PROPOSE":
                msg = new ACLMessage(ACLMessage.PROPOSE);
                msg.addReceiver(recipient);
                msg.setContent(message);
                sender.send(msg);
                System.out.println(sender.getName()+ " sent (" + message + ") to " + recipient.getName());
                break;
            case "":
                System.out.println("ACLType invalid/missing. Please enter correct type.");
                System.out.println("See jade documentation on ACLMessage FIPA Performative types.");
                break;
        }

    }
}
