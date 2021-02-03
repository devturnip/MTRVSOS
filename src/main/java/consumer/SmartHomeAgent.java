package consumer;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Maps;
import utils.Utils;

import java.util.*;

public class SmartHomeAgent extends Agent {
    private HashMap<String, Double> appliancesList = new HashMap<>();
    private double totalAppliancePowerConsumption = 0;
    private int rateSecs = 1000;
    private boolean hasInit = false;

    private double agent_X = 0;
    private double agent_Y = 0;
    private ImageView agentImageView;
    private int houseUnit = 100; //represents number of houses per agent

    private Maps mapsInstance = Maps.getMapsInstance();
    private Utils utility = new Utils();

    private LinkedHashMap<AID, Double> nearestNeighbours = new LinkedHashMap<>();
    private Map.Entry<AID, Double> nearestNeighbour = null;
    private ArrayList<Behaviour> behaviourList = new ArrayList<>();

    //message sending flags
    private AID currentNeighbour;
    private int retryCount = 0;
    private boolean messageSent = false;

    //colour flags
    private int currentColour = 0;
    private int ORANGE = 3;
    private int YELLOWGREEN = 4;

    //logs
    private static Logger LOGGER = LoggerFactory.getLogger(SmartHomeAgent.class);

    @Override
    protected void setup() {
        super.setup();
        initAppliances();
        LOGGER.info(getLocalName()+ "'s total appliances power consumption: "+ totalAppliancePowerConsumption);

        InitPosition initPosition = new InitPosition(this, 2000);
        ReceiveMessage receiveMessage = new ReceiveMessage();
        ConsumeElectricity consumeElectricity = new ConsumeElectricity(this, rateSecs);
        behaviourList.add(initPosition);
        behaviourList.add(receiveMessage);
        behaviourList.add(consumeElectricity);
        addBehaviour(initPosition);
        addBehaviour(receiveMessage);
        addBehaviour(consumeElectricity);
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        LOGGER.info(getLocalName() + " takedown. Killing...");
        try { DFService.deregister(this); }
        catch (Exception e) {}
        if(!behaviourList.isEmpty()) {
            for (Behaviour b: behaviourList){
                LOGGER.info("Removing behaviour(s): "+b);
                removeBehaviour(b);
            }
        }
        mapsInstance.removeUI(agentImageView);
        doDelete();
    }

    private void initAppliances() {
        Appliances appliances = new Appliances(10);
        appliancesList = appliances.initAppliances();
        Iterator iterator = appliancesList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            totalAppliancePowerConsumption = totalAppliancePowerConsumption + (Double)pair.getValue();
        }
        totalAppliancePowerConsumption = totalAppliancePowerConsumption * houseUnit;
    }

    private void initPosition() {
        HashMap<String, ImageView> hm = mapsInstance.getAgentMap(this.getLocalName(), true);
        HashMap.Entry<String, ImageView> entry = hm.entrySet().iterator().next();
        String agentName = entry.getKey();
        ImageView iv = entry.getValue();
        agentImageView = iv;
        agent_X = iv.getX();
        agent_Y = iv.getY();
        LOGGER.debug("THIS:" + this.getLocalName() + " agent:" + agentName + " X:" + agent_X + " Y:" + agent_Y);
    }

    private class InitPosition extends WakerBehaviour {
        public InitPosition(Agent a, long timeout) {
            super(a, timeout);
        }
        @Override
        protected void onWake() {
            super.onWake();
            initPosition();

            String[] servicesArgs = new String[] {"Power-Storage_Distribution", "Power-Generation"};
            nearestNeighbours = utility.getNearestObjectsList(this.myAgent, agent_X, agent_Y, servicesArgs);
            nearestNeighbour = utility.getNearest(this.myAgent, agent_X, agent_Y, servicesArgs);
            currentNeighbour = nearestNeighbour.getKey();

            hasInit = true;
        }
    }

    private class ConsumeElectricity extends TickerBehaviour {

        public ConsumeElectricity(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            HashMap.Entry<String, String> arguments = new HashMap.SimpleEntry<String, String>("toConsume", String.valueOf(totalAppliancePowerConsumption));
            if(hasInit== true) {
                if (messageSent == false) {
                    utility.sendMessageWithArgs(myAgent, currentNeighbour, arguments, "BEGIN_CONSUME", "REQUEST");
                    messageSent = true;
                } else if (messageSent){
                    retryCount = retryCount + 1;
                    LOGGER.debug("BLOCK");
                    if (retryCount == 5) {
                        //if blocked more than 5 times, recheck for nearest power source again.
                        if (currentColour != ORANGE) {
                            try {
                                mapsInstance.changeColor(agentImageView, "ORANGE");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            currentColour = ORANGE;
                        }
                        retryCount = 0;
                        String[] servicesArgs = new String[] {"Power-Storage_Distribution", "Power-Generation"};
                        currentNeighbour = utility.getNearestObjectsList(this.myAgent, agent_X, agent_Y, servicesArgs).keySet().iterator().next();
                        messageSent = false;
                    }
                    block();
                }
            }
        }
    }

    private class ReceiveMessage extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg!=null) {
                String content = msg.getContent();
                switch (content) {
                    case "ACCEPT_CONSUME":
                        messageSent = false;
                        if (currentColour != YELLOWGREEN) {
                            try {
                                mapsInstance.changeColor(agentImageView, "YELLOWGREEN");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            currentColour = YELLOWGREEN;
                        }
                        break;
                    case "REJECT_CONSUME":
                        messageSent = false;
                        //change status to orange
                        if (currentColour != ORANGE) {
                            try {
                                mapsInstance.changeColor(agentImageView, "ORANGE");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            currentColour = ORANGE;
                        }
                        //get next neighbour in list to send
                        HashMap.Entry<String, String> arguments = new HashMap.SimpleEntry<String, String>("toConsume", String.valueOf(totalAppliancePowerConsumption));
                        String[] servicesArgs = new String[] {"Power-Storage_Distribution", "Power-Generation"};
                        Iterator consumptionEngine = utility.getNearestObjectsList(this.myAgent, agent_X, agent_Y, servicesArgs).keySet().iterator();
                        AID first = utility.getNearestObjectsList(this.myAgent, agent_X, agent_Y, servicesArgs).keySet().iterator().next();
                        while (consumptionEngine.hasNext()) {
                            AID temp = (AID) consumptionEngine.next();
                            if (currentNeighbour.getLocalName().equals(temp.getLocalName()) && consumptionEngine.hasNext()){
                                currentNeighbour = (AID) consumptionEngine.next();
                                break;
                            } else if (!consumptionEngine.hasNext()) {
                                currentNeighbour = first;
                                break;
                            }
                        }
                        break;
                }
            }
            else {
                block();
            }
        }
    }
}
