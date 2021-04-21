package metamorphic;

import SoS.SoSAgent;
import com.opencsv.exceptions.CsvValidationException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import power.PowerGenAgent;
import utils.Utils;

import java.io.IOException;
import java.util.Random;

public class TestAgent extends Agent {
    private Utils utils = new Utils();
    private static Logger LOGGER = LoggerFactory.getLogger(PowerGenAgent.class);

    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new WakeTestAgentBehaviour(this, 30000));
        LOGGER.info(getLocalName() + " started.");
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }


    public class WakeTestAgentBehaviour extends WakerBehaviour{

        public WakeTestAgentBehaviour(Agent a, long timeout) {
            super(a, timeout);
        }

        @Override
        protected void onWake() {
            super.onWake();
            myAgent.addBehaviour(new MRReliableBehaviour());
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
            AID[] agents = utils.getAgentNamesByService(myAgent, "Power-Generation");
            float probability = 100.0F;
            for (AID agent:agents) {
                boolean toCrash = getRandomBoolean(probability);
                if(toCrash) {
                    utils.sendMessage(myAgent, agent, "SIMULATE_FAIL", "INFORM");
                    long downTime = 0;
                    try {
                        downTime = utils.getOutageDownTime();
                        addBehaviour(new RecoverAgent(myAgent, downTime, agent));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (CsvValidationException e) {
                        e.printStackTrace();
                    }
                    LOGGER.info(getLocalName() + " sent SIMULATE_FAIL for " + downTime + " to " + agent.getLocalName());
                    probability = probability/2; //halves failure everytime a failure is triggered.
                } else {
                    LOGGER.info(getLocalName() + " skipped SIMULATE_FAIL for " + agent.getLocalName());
                }
            }

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

    private boolean getRandomBoolean(float probability) {
        double randomValue = Math.random()*100;
        return randomValue <= probability;
    }

}
