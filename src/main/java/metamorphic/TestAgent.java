package metamorphic;

import com.opencsv.exceptions.CsvValidationException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import power.Power;
import power.PowerGenAgent;
import utils.ElasticHelper;
import utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class TestAgent extends Agent {
    private Utils utils = new Utils();
    private static Logger LOGGER = LoggerFactory.getLogger(PowerGenAgent.class);
    private Power powerInstance = Power.getPowerInstance();
    private Behaviour behaviour = null;
    private ElasticHelper elasticHelper = ElasticHelper.getElasticHelperInstance();
    private ArrayList<AID> checkedAgents = new ArrayList<>();

    @Override
    protected void setup() {
        super.setup();
        behaviour = new CheckPowerUtilisation(this, 1000);
        addBehaviour(behaviour);

        LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
        logArgs.put("action", "test_agent.init");
        logArgs.put("test_type", behaviour.getBehaviourName());
        String message = getLocalName() + " started.";
        LOGGER.info(message);
        elasticHelper.indexLogs(this, logArgs);
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }

    public class MRReliableBehaviourRealProbability extends OneShotBehaviour {
        //no retry mechanism; this behaviour runs once per simulation
        //agents have a x probability chance of failure (based on 'real-er' data)
        @Override
        public void action() {
            //get list of power agents
            AID[] agents = utils.getAgentNamesByService(myAgent, "Power-Generation");
            boolean toCrash = false;
            //for each agent
            for (AID agent : agents) {
                try {
                    //calculate their probability of failure
                    toCrash = getRandomBoolean((float) utils.getOutageProbability());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (CsvValidationException e) {
                    e.printStackTrace();
                }
                //if fail
                if (toCrash) {
                    //send message to power agent to fail
                    utils.sendMessage(myAgent, agent, "SIMULATE_FAIL", "INFORM");
                    long downTime = 0;
                    try {
                        //get outage downtime from csv list
                        downTime = utils.getOutageDownTime();

                        //add behaviour to send recovery message based on downtime
                        addBehaviour(new RecoverAgent(myAgent, downTime, agent));

                        //log agents who received failed message
                        LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
                        logArgs.put("action", "test_agent.test");
                        logArgs.put("test_type", behaviour.getBehaviourName());
                        logArgs.put("simulate_fail", "true");
                        logArgs.put("downtime", String.valueOf(downTime));
                        logArgs.put("receiver", agent.getLocalName());
                        String message = getLocalName() + " sent SIMULATE_FAIL for " + downTime + " to " + agent.getLocalName();
                        LOGGER.info(message);

                        elasticHelper.indexLogs(getAgent(), logArgs);
                    } catch (CsvValidationException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LOGGER.info(getLocalName() + " skipped SIMULATE_FAIL for " + agent.getLocalName());
                }
            }
        }
    }

    public class MRReliableBehaviour extends OneShotBehaviour{
        @Override
        public void action() {
            /*
            Check for manipulable agents.
            Manipulate state during runtime based on probability.
            Calculate time for which agents should remain down
                -based on actual data or some function?
            Restore agent state after calculated time has elapsed.
             */
            //all power agents will fail due to retry mechanism
            //use this if you need all power agents to fail
            if(behaviour != null) {
                LOGGER.debug("Removing " + behaviour.getBehaviourName());
                removeBehaviour(behaviour);
            }
            AID[] agents = utils.getAgentNamesByService(myAgent, "Power-Generation");
            float probability = 100.0F;
            for (AID agent:agents) {
                if (!checkedAgents.contains(agent)) {
                    boolean toCrash = getRandomBoolean(probability);
                    if(toCrash) {
                        utils.sendMessage(myAgent, agent, "SIMULATE_FAIL", "INFORM");
                        long downTime = 0;
                        try {
                            downTime = utils.getOutageDownTime();
                            addBehaviour(new RecoverAgent(myAgent, downTime, agent));
                            checkedAgents.add(agent);

                            LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
                            logArgs.put("action", "test_agent.test");
                            logArgs.put("test_type", behaviour.getBehaviourName());
                            logArgs.put("simulate_fail", "true");
                            logArgs.put("downtime", String.valueOf(downTime));
                            logArgs.put("receiver", agent.getLocalName());
                            String message = getLocalName() + " sent SIMULATE_FAIL for " + downTime + " to " + agent.getLocalName();
                            LOGGER.info(message);
                            elasticHelper.indexLogs(getAgent(), logArgs);

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (CsvValidationException e) {
                            e.printStackTrace();
                        }
                        probability = probability/2; //halves failure everytime a failure is triggered.

                    } else {
                        LOGGER.info(getLocalName() + " skipped SIMULATE_FAIL for " + agent.getLocalName());
                    }
                }
            }

            //retry mechanism
            behaviour = new CheckPowerUtilisation(myAgent,1000);
            addBehaviour(behaviour);

        }
    }

    public class ConditionalMRReliableBehaviour extends OneShotBehaviour{
        @Override
        public void action() {
             /*
            Includes conditional testing
            Check for manipulable agents.
            Manipulate state during runtime based on probability.
            Calculate time for which agents should remain down
                -based on actual data or some function?
            Restore agent state after calculated time has elapsed.
             */
        }
    }

    public class RecoverAgent extends WakerBehaviour{
        private AID receiver;
        public RecoverAgent(Agent a, long timeout, AID receiver) {
            super(a, timeout);
            this.receiver = receiver;
        }

        @Override
        protected void onWake() {
            super.onWake();
            utils.sendMessage(getAgent(), receiver, "SIMULATE_RECOVER", "INFORM");
            LOGGER.info(getLocalName() + " sent SIMULATE_RECOVER to " + receiver.getLocalName());
        }
    }

    public class CheckPowerUtilisation extends TickerBehaviour {
        private boolean didCheck = false;

        public CheckPowerUtilisation(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            double totalPowerLevels = powerInstance.getGridMax();
            double currentPowerLevels = powerInstance.showPowerLevels();
            double powerPercentage = (currentPowerLevels/totalPowerLevels)*100;
            if (powerPercentage > 80 && !didCheck) {
                addBehaviour(new MRReliableBehaviourRealProbability());
                didCheck = true;
            }
        }
    }

    private boolean getRandomBoolean(float probability) {
        double randomValue = Math.random()*100;
        return randomValue <= probability;
    }

}
