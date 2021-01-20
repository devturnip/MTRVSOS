package consumer;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import javafx.scene.image.ImageView;
import utils.Maps;
import utils.Utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class EVAgent extends Agent {
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

    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new InitPosition(this, 2000));
    }

    @Override
    protected void takeDown() {
        super.takeDown();
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

        }
    }
}
