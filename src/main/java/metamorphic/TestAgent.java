package metamorphic;

import SoS.SoSAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import power.PowerGenAgent;
import utils.Utils;

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
            utils.sendMessage(myAgent, agents[0], "SIMULATE_FAIL", "INFORM");
            addBehaviour(new RecoverAgent(myAgent, 10000, agents[0]));
            LOGGER.info(getLocalName() + " sent SIMULATE_FAIL to " + agents[0].getLocalName());
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

}
