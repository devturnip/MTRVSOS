package metamorphic;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

public class TestAgent extends Agent {
    @Override
    protected void setup() {
        super.setup();
    }

    @Override
    protected void takeDown() {
        super.takeDown();
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
        }
    }

}
