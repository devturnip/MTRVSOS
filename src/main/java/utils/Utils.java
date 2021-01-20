package utils;

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

import java.util.*;

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

    public LinkedHashMap<AID, Double> getNearestObjectsList (Agent agent1, double ax, double ay, String serviceName) {
        AID[] agents = getAgentNamesByService(agent1, serviceName);
        ArrayList<AID> agentsList = new ArrayList<AID>(Arrays.asList(agents));
        ArrayList<AID> tempAgentsList = new ArrayList<AID>(Arrays.asList(agents));
        LinkedHashMap<AID, Double> retMap = new LinkedHashMap<AID, Double>();

        while (retMap.size() != agentsList.size()) {
            HashMap.Entry<AID, Double> near = getNearest(agent1, ax, ay, tempAgentsList);
            tempAgentsList.remove(near.getKey());
            retMap.put(near.getKey(), near.getValue());

        }
        return retMap;
    }

    public LinkedHashMap<AID, Double> getNearestObjectsList (Agent agent1, double ax, double ay, String[] serviceName) {
        ArrayList<AID[]> arrayOfAgents = new ArrayList<>();

        for (int i=0; i<serviceName.length; i++){
            arrayOfAgents.add(getAgentNamesByService(agent1, serviceName[i]));
        }

        ArrayList<AID> fullAgentsList  = new ArrayList<>();

        for (int i=0; i<arrayOfAgents.size(); i++){
            AID[] tempAID = arrayOfAgents.get(i);
            for (int x=0; x<tempAID.length; x++) {
                fullAgentsList.add(tempAID[x]);
                //System.out.println("FULL AGENT LIST:" + tempAID[x]);
            }
        }

        ArrayList<AID> tempAgentsList = fullAgentsList;
        LinkedHashMap<AID, Double> retMap = new LinkedHashMap<AID, Double>();

        while (retMap.size() <= fullAgentsList.size()) {
            HashMap.Entry<AID, Double> near = getNearest(agent1, ax, ay, tempAgentsList);
            tempAgentsList.remove(near.getKey());
            retMap.put(near.getKey(), near.getValue());
            //System.out.println("NEAR:" + near.getKey());

        }
        return retMap;
    }

    public HashMap.Entry<AID, Double> getNearest(Agent agent1, double ax, double ay, ArrayList<AID> agents) {
        double tempDistance = 0;
        double retDistance = 0;
        AID retAgent = null;

        Maps mapInstance = Maps.getMapsInstance();
        Point2D agent1_loc = new Point2D(ax, ay);
        Point2D agent2_loc;

        for (int i=0; i<agents.size(); i++) {
            HashMap.Entry<String, ImageView> entry = mapInstance.getAgentMap(agents.get(i).getLocalName(), true).entrySet().iterator().next();
            agent2_loc = new Point2D(entry.getValue().getX(), entry.getValue().getY());
            tempDistance = agent1_loc.distance(agent2_loc);
            if (retDistance == 0) {
                retDistance = tempDistance;
                retAgent = agents.get(i);
            } else if (tempDistance < retDistance) {
                retDistance = tempDistance;
                retAgent = agents.get(i);
            }
        }

//        System.out.println("SHORTEST Distance between " + agent1.getLocalName() + " and " +
//                retAgent.getLocalName() + " is " + retDistance);

        HashMap.Entry<AID, Double> retMap = new HashMap.SimpleEntry<AID, Double>(retAgent, Double.valueOf(retDistance));
        return retMap;
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

//        System.out.println("SHORTEST Distance between " + agent1.getLocalName() + " and " +
//                retAgent.getLocalName() + " is " + retDistance);

        HashMap.Entry<AID, Double> retMap = new HashMap.SimpleEntry<AID, Double>(retAgent, Double.valueOf(retDistance));
        return retMap;
    }

    public HashMap.Entry<AID, Double> getNearest(Agent agent1, double ax, double ay, String[] serviceNames) {
        double tempDistance = 0;
        double retDistance = 0;
        AID retAgent = null;

        Maps mapInstance = Maps.getMapsInstance();
        Point2D agent1_loc = new Point2D(ax, ay);
        Point2D agent2_loc;

        ArrayList<AID[]> arrayOfAgents = new ArrayList<>();
        for (int i=0; i<serviceNames.length; i++) {
            arrayOfAgents.add(getAgentNamesByService(agent1, serviceNames[i]));
        }

        ArrayList<AID> fullAgentsList  = new ArrayList<>();
        for (int i=0; i<arrayOfAgents.size(); i++){
            AID[] tempAID = arrayOfAgents.get(i);
            for (int x=0; x<tempAID.length; x++) {
                fullAgentsList.add(tempAID[x]);
                //System.out.println("FULL AGENT LIST:" + tempAID[x]);
            }
        }

        for (int i=0; i<fullAgentsList.size(); i++) {
            HashMap.Entry<String, ImageView> entry = mapInstance.getAgentMap(fullAgentsList.get(i).getLocalName(), true).entrySet().iterator().next();
            agent2_loc = new Point2D(entry.getValue().getX(), entry.getValue().getY());
            tempDistance = agent1_loc.distance(agent2_loc);
            //System.out.println("Distance between " + agent1.getLocalName() + " and " +
            //agents[i].getLocalName() + " is " + tempDistance);
            if (retDistance == 0) {
                retDistance = tempDistance;
                retAgent = fullAgentsList.get(i);
            } else if (tempDistance < retDistance) {
                retDistance = tempDistance;
                retAgent = fullAgentsList.get(i);
            }
        }
//        System.out.println("SHORTEST Distance between " + agent1.getLocalName() + " and " +
//                retAgent.getLocalName() + " is " + retDistance);

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
                //System.out.println(sender.getName()+ " sent (" + message + ") to " + recipient.getName());
                break;
            case "PROPOSE":
                msg = new ACLMessage(ACLMessage.PROPOSE);
                msg.addReceiver(recipient);
                msg.setContent(message);
                sender.send(msg);
                //System.out.println(sender.getName()+ " sent (" + message + ") to " + recipient.getName());
                break;
            case "":
                System.out.println("ACLType invalid/missing. Please enter correct type.");
                System.out.println("See jade documentation on ACLMessage FIPA Performative types.");
                break;
        }

    }

    public void sendMessageWithArgs (Agent sender, AID recipient, Map.Entry<String, String> userDefinedParams, String message, String ACLTYPE) {
        ACLMessage msg = null;
        switch (ACLTYPE) {
            case "CFP":
                msg = new ACLMessage(ACLMessage.CFP);
                msg.addReceiver(recipient);
                msg.setContent(message);
                msg.addUserDefinedParameter(userDefinedParams.getKey(), userDefinedParams.getValue());
                sender.send(msg);
                //System.out.println(sender.getName()+ " sent (" + message + ") to " + recipient.getName() + " with args: " + userDefinedParams.getKey() + ":" + userDefinedParams.getValue());
                break;
            case "PROPOSE":
                msg = new ACLMessage(ACLMessage.PROPOSE);
                msg.addReceiver(recipient);
                msg.setContent(message);
                msg.addUserDefinedParameter(userDefinedParams.getKey(), userDefinedParams.getValue());
                sender.send(msg);
                //System.out.println(sender.getName()+ " sent (" + message + ") to " + recipient.getName() + " with args: " + userDefinedParams.getKey() + ":" + userDefinedParams.getValue());
                break;
            case "REQUEST":
                msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(recipient);
                msg.setContent(message);
                msg.addUserDefinedParameter(userDefinedParams.getKey(), userDefinedParams.getValue());
                sender.send(msg);
                //System.out.println(sender.getName()+ " sent (" + message + ") to " + recipient.getName() + " with args: " + userDefinedParams.getKey() + ":" + userDefinedParams.getValue());
                break;
            case "":
                System.out.println("ACLType invalid/missing. Please enter correct type.");
                System.out.println("See jade documentation on ACLMessage FIPA Performative types.");
                break;
        }

    }
}
