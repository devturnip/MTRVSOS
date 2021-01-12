package power;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import javafx.scene.image.ImageView;
import utils.Maps;
import utils.Utils;

import java.util.HashMap;
import java.util.Random;

public class PowerStoreDisAgent extends Agent {
    private double maxCapacity = 0;
    private double holdCapacity = 0;
    private double agent_X = 0;
    private double agent_Y = 0;
    private ImageView agentImageView;

    private Maps mapsInstance = Maps.getMapsInstance();
    private Power powerInstance = Power.getPowerInstance();

    //message sending flags
    private int countCFP = 0;

    @Override
    protected void setup() {
        super.setup();
        determineCapacity();
        System.out.println(getAID().getName() + " started with capacity of " + maxCapacity);

        Utils utils = new Utils();
        utils.registerServices(this, "Power-Storage_Distribution");

        addBehaviour(new InitPosition(this, 2000));
        addBehaviour(new ReceiveMessage());
    }

    private void determineCapacity() {
        maxCapacity = new Random().ints(100000, 500000).findFirst().getAsInt();
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
        }
    }

    private class GeneratePower extends OneShotBehaviour {
        private String toAdd = "";
        public GeneratePower(String toAdd) {
            super();
            this.toAdd = toAdd;
        }
        @Override
        public void action() {
            if (holdCapacity < maxCapacity) {
                double addTo = Double.parseDouble(toAdd);
                powerInstance.addPowerLevel(addTo);
                holdCapacity = holdCapacity + addTo;
                System.out.println(myAgent.getLocalName() + " total power levels: " + String.valueOf(holdCapacity));
                try {
                    mapsInstance.addHue(agentImageView, "GREEN");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (holdCapacity >= maxCapacity) {
                maxCapacity = holdCapacity;
                System.out.println("Max capacity at " + maxCapacity + " of " + getName() + " . Paused.");
                try {
                    mapsInstance.addHue(agentImageView, "BLUE");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ReceiveMessage extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();

            if (msg != null) {
                String contents = msg.getContent();
                int performative = msg.getPerformative();
                switch (performative) {
                    case 11:
                        contents = msg.getContent();
                        if (contents.equals("BEGIN_STORE")) {
                            System.out.println(myAgent.getLocalName() + " received PROPOSE (" + contents + ") from " + msg.getSender().getLocalName());
                            ACLMessage reply = msg.createReply();
                            if (holdCapacity == 0 || holdCapacity < maxCapacity) {
                                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                reply.setContent("ACCEPT_STORE");
                                send(reply);
                            } else if (holdCapacity >= maxCapacity) {
                                reply.setPerformative(ACLMessage.REFUSE);
                                reply.setContent("REJECT_STORE");
                                send(reply);
                            }
                        }
                        break;
                    case 16:
                        contents = msg.getContent();
                        switch(contents) {
                            case "ADD":
                                String key = (String)msg.getAllUserDefinedParameters().entrySet().iterator().next().getKey();
                                String value = (String)msg.getAllUserDefinedParameters().entrySet().iterator().next().getValue();
                                if(holdCapacity == 0 || holdCapacity < maxCapacity) {
                                    myAgent.addBehaviour(new GeneratePower(value));
                                } else if (holdCapacity >= maxCapacity) {
                                    myAgent.addBehaviour(new GeneratePower(value));
                                    ACLMessage replyBack = msg.createReply();
                                    replyBack.setPerformative(ACLMessage.REFUSE);
                                    replyBack.setContent("REJECT_STORE");
                                    send(replyBack);

                                }
                                break;
                        }
                }
            } else {
                block();
            }
        }
    }
}
