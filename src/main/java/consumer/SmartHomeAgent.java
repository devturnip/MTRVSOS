package consumer;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import javafx.scene.image.ImageView;
import power.PowerStoreDisAgent;
import utils.Maps;
import utils.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class SmartHomeAgent extends Agent {
    private HashMap<String, Double> appliancesList = new HashMap<>();
    private double totalAppliancePowerConsumption = 0;
    private int rateSecs = 1000;
    private boolean hasInit = false;

    private double agent_X = 0;
    private double agent_Y = 0;
    private ImageView agentImageView;

    private Maps mapsInstance = Maps.getMapsInstance();
    private Utils utility = new Utils();

    private LinkedHashMap<AID, Double> nearestNeighbours = new LinkedHashMap<>();
    private Map.Entry<AID, Double> nearestNeighbour = null;

    //message sending flags
    private AID currentNeighbour;
    private AID nextNeighbour;
    private long retryTime = 2000;
    private boolean messageSent = false;
    private boolean acceptSent = true;

    //colour flags
    private int currentColour = 0;
    private int GREEN = 1;
    private int ORANGE = 3;
    private int YELLOWGREEN = 4;

    @Override
    protected void setup() {
        super.setup();
        initAppliances();
        System.out.println(getLocalName()+ "'s total power consumption: "+ totalAppliancePowerConsumption);
        addBehaviour(new SmartHomeAgent.InitPosition(this, 2000));
        addBehaviour(new ReceiveMessage2());
        addBehaviour(new ConsumeElectricity2(this, rateSecs));
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        System.out.println(getLocalName() + " takedown. Killing...");
        try { DFService.deregister(this); }
        catch (Exception e) {}
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
    }

    private void initPosition() {
        HashMap<String, ImageView> hm = mapsInstance.getAgentMap(this.getLocalName(), true);
        HashMap.Entry<String, ImageView> entry = hm.entrySet().iterator().next();
        String agentName = entry.getKey();
        ImageView iv = entry.getValue();
        agentImageView = iv;
        agent_X = iv.getX();
        agent_Y = iv.getY();
        System.out.println("THIS:" + this.getLocalName() + " agent:" + agentName + " X:" + agent_X + " Y:" + agent_Y);
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
    private class ConsumeElectricity2 extends TickerBehaviour {

        public ConsumeElectricity2(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            HashMap.Entry<String, String> arguments = new HashMap.SimpleEntry<String, String>("toConsume", String.valueOf(totalAppliancePowerConsumption));
            String[] servicesArgs = new String[] {"Power-Storage_Distribution", "Power-Generation"};
            if (hasInit == true) {
                if (acceptSent) {
                    utility.sendMessageWithArgs(myAgent, currentNeighbour, arguments, "BEGIN_CONSUME", "REQUEST");
                } else {
                    Iterator consumptionEngine = utility.getNearestObjectsList(this.myAgent, agent_X, agent_Y, servicesArgs).keySet().iterator();
                    int count =0;
                    while (consumptionEngine.hasNext()) {
                        count+=1;
                        AID temp = (AID) consumptionEngine.next();
                        System.out.println("Loop:"+count+" temp:" + temp.getLocalName() + " current:" + currentNeighbour.getLocalName());
                        if (temp.getLocalName().equals(currentNeighbour.getLocalName()) && consumptionEngine.hasNext()) { //if first item equals to current neighbour
                            nextNeighbour = (AID)consumptionEngine.next();
                            //send to next neighbour in list.
                            utility.sendMessageWithArgs(myAgent, nextNeighbour, arguments, "BEGIN_CONSUME", "REQUEST");
                            //current neighbour is next neighbour.
                            currentNeighbour = nextNeighbour;
                            System.out.println("Break from loop:"+count);
                            break;
                        }
                        else if (!consumptionEngine.hasNext()) { //if last item in list
                            //reset current neighbour to first item in list
                            currentNeighbour = utility.getNearestObjectsList(this.myAgent, agent_X, agent_Y, servicesArgs).keySet().iterator().next();
                            utility.sendMessageWithArgs(myAgent, currentNeighbour, arguments, "BEGIN_CONSUME", "REQUEST");
                            System.out.println("Break from loop:"+count);
                            acceptSent = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    private class ReceiveMessage2 extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg!=null) {
                String content = msg.getContent();
                switch (content) {
                    case "ACCEPT_CONSUME":
//                        System.out.println("TRUE");
                        acceptSent = true;
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
//                        System.out.println("FALSE");
                        acceptSent = false;
                        if (currentColour != ORANGE) {
                            try {
                                mapsInstance.changeColor(agentImageView, "ORANGE");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            currentColour = ORANGE;
                        }
                        break;
                }
            } else {
                block();
            }
        }
    }
}
