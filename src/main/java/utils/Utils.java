package utils;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Utils {
    //logs
    private static Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private InputStream inputStream = null;
    private ArrayList<Double> powerGenCapacityValues = new ArrayList<>();
    private ArrayList<Double> powerStorageCapacityValues = new ArrayList<>();

    public Utils() {
    }

    public void searchForAgent(String agentName){
        AMSAgentDescription[] amsAgentDescriptions = null;

        SearchConstraints searchConstraints = new SearchConstraints();
        searchConstraints.setMaxResults(new Long(1));

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

    public void registerServices(Agent a, String[] serviceType) {
        String fullServiceName = a.getLocalName() + serviceType;
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(a.getAID());
        ServiceDescription sd = null;

        for (int i=0; i< serviceType.length; i++) {
            sd = new ServiceDescription();
            String serviceName = serviceType[i];
            sd.setType(serviceName);
            sd.setName(fullServiceName);
            dfd.addServices(sd);
        }
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

    public HashMap.Entry<AID, Double> getNearest(Agent agent1, double ax, double ay, String serviceName, AID previousNeighbour) {
        LOGGER.debug("GET NEAREST CALLED, PREVIOUS:" + previousNeighbour.getLocalName());
        double tempDistance = 0;
        double retDistance = 0;
        AID retAgent = null;

        Maps mapInstance = Maps.getMapsInstance();
        Point2D agent1_loc = new Point2D(ax, ay);
        Point2D agent2_loc;
        AID[] agents = getAgentNamesByService(agent1, serviceName);
        ArrayList<AID> agentsAL = new ArrayList<>();

        for (AID agent:agents) {
            agentsAL.add(agent);
        }

        agentsAL.remove(previousNeighbour);

        for (int i=0; i<agentsAL.size(); i++) {
            HashMap.Entry<String, ImageView> entry = mapInstance.getAgentMap(agentsAL.get(i).getLocalName(), true).entrySet().iterator().next();
            agent2_loc = new Point2D(entry.getValue().getX(), entry.getValue().getY());
            tempDistance = agent1_loc.distance(agent2_loc);
            if (retDistance == 0) {
                retDistance = tempDistance;
                retAgent = agentsAL.get(i);
            } else if (tempDistance < retDistance) {
                retDistance = tempDistance;
                retAgent = agentsAL.get(i);
            }
        }

        LOGGER.info("SHORTEST Distance between " + agent1.getLocalName() + " and " +
                retAgent.getLocalName() + " is " + retDistance);

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
            case "REFUSE":
                msg = new ACLMessage(ACLMessage.REFUSE);
                msg.addReceiver(recipient);
                msg.setContent(message);
                sender.send(msg);
                //System.out.println(sender.getName()+ " sent (" + message + ") to " + recipient.getName());
                break;
            case "":
                LOGGER.warn("ACLType invalid/missing. Please enter correct type.");
                LOGGER.warn("See jade documentation on ACLMessage FIPA Performative types.");
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
                System.out.println(sender.getName()+ " sent (" + message + ") to " + recipient.getName() + " with args: " + userDefinedParams.getKey() + ":" + userDefinedParams.getValue());
                break;
            case "":
                LOGGER.warn("ACLType invalid/missing. Please enter correct type.");
                LOGGER.warn("See jade documentation on ACLMessage FIPA Performative types.");
                break;
        }
    }

    public double getPowerGenRate() throws IOException, CsvValidationException {
        if (powerGenCapacityValues.size() == 0) {
            inputStream = this.getClass().getClassLoader().getResourceAsStream("data/netgen_10k.csv");
            if (inputStream == null) {
                LOGGER.error("Resource does not exist");
            }
            else {
                CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream));
                csvReader.skip(1);
                String[] nextLine;
                while ((nextLine = csvReader.readNext()) != null) {
                    if (nextLine != null) {
                        //ignore header
                        //LOGGER.debug(Arrays.toString(nextLine));
                        powerGenCapacityValues.add(Double.parseDouble(nextLine[1]));
                        powerGenCapacityValues.add(Double.parseDouble(nextLine[2]));
                        powerGenCapacityValues.add(Double.parseDouble(nextLine[3]));
                    }
                }
            }
        }
        double returnValue = powerGenCapacityValues.get(new Random().nextInt(new Random().ints(1, powerGenCapacityValues.size()).findFirst().getAsInt()));
        returnValue = (returnValue/30)/24; //divide by 30 days, divide by 24 hours to return net generation in mwh
        return returnValue;

    }

    public double getStorageCapacity() throws IOException, CsvValidationException {
        if (powerStorageCapacityValues.size() == 0){
            inputStream = this.getClass().getClassLoader().getResourceAsStream("data/storage_capacity.csv");
            if (inputStream == null) {
                LOGGER.error("Resource does not exist");
            }
            else {
                CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream));
                csvReader.skip(1);
                String[] nextLine;
                while ((nextLine = csvReader.readNext()) != null) {
                    if (nextLine != null) {
                        //ignore header
                        //LOGGER.debug(nextLine[3]);
                        powerStorageCapacityValues.add(Double.parseDouble(nextLine[3]));
                    }
                }
            }
        }
        double returnValue = powerStorageCapacityValues.get(new Random().nextInt(new Random().ints(1,powerStorageCapacityValues.size()).findFirst().getAsInt()));
        returnValue = returnValue*1000; //return in kwh
        return returnValue;
    }
}
