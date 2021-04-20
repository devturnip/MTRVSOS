package SoS;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import power.Power;
import utils.Settings;
import utils.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;

public class SoSAgent extends Agent {
    //logs
    private static Logger LOGGER = LoggerFactory.getLogger(SoSAgent.class);

    private HashMap<AID, ACLMessage> messageHashMap = new HashMap<>();
    ArrayList<String> messageQueue = new ArrayList<>();
    Power powerInstance = Power.getPowerInstance();
    Settings settingsInstance = Settings.getSettingsInstance();

    double preferredUtilisationRate = settingsInstance.getPreferredUtilisationRate();
    double preferredIncrement = settingsInstance.getPreferredIncrement();
    double powerUtilisationRate = settingsInstance.getPowerUtilisationRate();

    int msg_reject_count = 0;
    private boolean pauseAgent = false;


    @Override
    protected void setup() {
        LOGGER.info(getLocalName() + " started.");
        addBehaviour(new ContactPower(this,12000, "START"));
        addBehaviour(new ReceiveMessage());
        addBehaviour(new CheckSimulationState(this, settingsInstance.getSimCheckRate()));
        //addBehaviour(new ContactPower(this,20000, "STOP"));
    }

    private class ContactPower extends WakerBehaviour {
        private String message;
        public ContactPower(Agent a, long timeout, String message) {
            super(a, timeout);
            this.message = message;
        }

        @Override
        protected void onWake() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

            Utils util = new Utils();
            AID[] agents = util.getAgentNamesByService(SoSAgent.this, "Power-Generation");
            for (int i=0; i<agents.length; i++) {
                //System.out.println(agents[i]);
                if (agents[i] != null) {
                    msg.addReceiver(agents[i]);
                    msg.setContent(message);
                    send(msg);
                    LOGGER.info(this.myAgent.getName()+ " sent (" + message + ") to " + agents[i].getName());
                }
            }

            addBehaviour(new AdjustUtilisationRate(myAgent, 1000));
        }
    }

    private class AdjustUtilisationRate extends TickerBehaviour {

        public AdjustUtilisationRate(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (pauseAgent == false) {
                double totalPowerLevels = powerInstance.getGridMax();
                double currentPowerLevels = powerInstance.showPowerLevels();
                double powerPercentage = (currentPowerLevels/totalPowerLevels)*100;
                double demandRate = powerInstance.getDemand();
                double genRate = powerInstance.getGenRate();

                double gdRate = (demandRate / genRate) * 100;
                double utRate = -1.0;
                if (powerPercentage>powerUtilisationRate) {
                    //if total power reserves are greater than set value
                    if (Double.isFinite(gdRate)) {
                        BigDecimal bigDecimal = new BigDecimal(gdRate).setScale(2, RoundingMode.HALF_UP);
                        utRate = bigDecimal.doubleValue();
                    }

                    if(utRate >= 90 && utRate < 100) {
                        preferredIncrement = 0.01;
                    } else if (utRate >= 95 && utRate < 100) {
                        preferredIncrement = 0.001;
                    } else if (utRate > 100 && utRate < 110) {
                        preferredIncrement = 0.01;
                    } else if (utRate > 100 && utRate < 105) {
                        preferredIncrement = 0.001;
                    }
                    else{
                        preferredIncrement = settingsInstance.getPreferredIncrement();
                    }

                    if (utRate >= preferredUtilisationRate && utRate <= 100) {
                        block();
                    }
                    else {
                        if (demandRate < genRate) {
                            Utils utils = new Utils();
                            AID[] agents = utils.getAgentNamesByService(SoSAgent.this, "Power-Generation");

                            for (int i = 0; i < agents.length; i++) {
                                if (!messageQueue.contains(agents[i].getLocalName())) {
                                    String content = "GENRATE_DECR";
                                    HashMap.Entry<String, String> arguments = new HashMap.SimpleEntry<String, String>("toAdd", String.valueOf(preferredIncrement));
                                    utils.sendMessageWithArgs(myAgent, agents[i], arguments, content, "REQUEST");
                                    messageQueue.add(agents[i].getLocalName());
                                    //LOGGER.warn("AGENTSLOCALNAME: " + agents[i].getLocalName());
                                }
                            }
                        }
                    }

                } else if (powerPercentage <= 40) {
                    genRateInc();
                }
                else {
                    block();
                }
            }
            else {
                block();
            }
        }

        private void genRateInc() {
            Utils utils = new Utils();
            AID[] agents = utils.getAgentNamesByService(SoSAgent.this, "Power-Generation");

            for (int i = 0; i < agents.length; i++) {
                if (!messageQueue.contains(agents[i].getLocalName())) {
                    String content = "GENRATE_INCR";
                    HashMap.Entry<String, String> arguments = new HashMap.SimpleEntry<String, String>("toAdd", String.valueOf(preferredIncrement));
                    utils.sendMessageWithArgs(myAgent, agents[i], arguments, content, "REQUEST");
                    messageQueue.add(agents[i].getLocalName());
                    //LOGGER.warn("AGENTSLOCALNAME: " + agents[i].getLocalName());
                }
            }
        }
    }

    private class ReceiveMessage extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage message = myAgent.receive();
            if (message != null) {
                String contents = message.getContent();
                switch (contents) {
                    case "INCR_ACCEPTED":
                    case "DECR_ACCEPTED":
                        //LOGGER.warn("SENDERLOCALNAME: " + message.getSender().getLocalName());
                        messageQueue.remove(message.getSender().getLocalName());
                        break;
                    case "INCR_REJECTED":
                        messageQueue.remove(message.getSender().getLocalName());
                    case "DECR_REJECTED":
                        if (msg_reject_count >= 5) {
                            messageQueue.remove(message.getSender().getLocalName());
                        } else {
                            msg_reject_count = msg_reject_count++;
                        }
                        break;
                }

            } else {
                block();
            }
        }
    }

    private class CheckSimulationState extends TickerBehaviour{
        public CheckSimulationState(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            pauseAgent = settingsInstance.getSimulationState();
        }
    }

    private class KillPowerGenAgent extends OneShotBehaviour {

        @Override
        public void action() {

        }
    }
}
