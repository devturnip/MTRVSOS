package consumer;

import com.sun.javafx.geom.Point2D;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import utils.Maps;
import utils.Utils;

import java.util.*;

public class EVAgent extends Agent {
    private double agent_X = 0;
    private double agent_Y = 0;
    private ImageView agentImageView;
    private double canvas_x = 1280;
    private double canvas_y = 1020;

    private Maps mapsInstance = Maps.getMapsInstance();
    private Utils utility = new Utils();

    private LinkedHashMap<AID, Double> nearestNeighbours = new LinkedHashMap<>();
    private Map.Entry<AID, Double> nearestNeighbour = null;
    private HashMap<String, Point2D> listOfAgentPositions = new HashMap<>();

    //message sending flags
    private AID currentNeighbour;
    private AID nextNeighbour;

    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new InitPosition(this, 2000));
        addBehaviour(new UpdatePositionList(this, 500));
        addBehaviour(new MoveCar(this, 4000));

        /*
        EV Behaviour writeup:
        Randomly pick 2 separate nodes within specified distance. (avg travel distance to be surveyed from studies.
        each unit of point travelled should represent some distance: 0,0 - 0,1
        - 100kWh battery in a Tesla Model S (above) is capable of delivering a maximum of 100 kilowatts of energy for one hour straight.

        Others:
        JavaFX uses reverse cartesian coordinates system; 0,0 means upper left corner.
        As Y values increases, point moves downwards from top.
         */
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
    private void updateSelfPosition() {
        ImageView iv = agentImageView;
        int x_position = (int) iv.getX();
        int y_position = (int) iv.getY();
        Point2D updatedPoint2d = new Point2D(x_position, y_position);
        mapsInstance.mapAgentLocation(getLocalName(), updatedPoint2d);
    }

    private void travel() {
        final Point2D[] currentPoint = {new Point2D((int) agent_X, (int) agent_Y)};
        Point2D destPoint = new Point2D();

        while (true) {
            int x0 = new Random().ints(0, ((int)(canvas_x-agentImageView.getFitHeight()))).findFirst().getAsInt();
            int y0 = new Random().ints(0, ((int)(canvas_y-agentImageView.getFitWidth()))).findFirst().getAsInt();
            destPoint = new Point2D(x0,y0);
            if (currentPoint[0].distance(destPoint) > 500) {
                System.out.println("DEST:"+x0+":"+y0);
                break;
            }
        }

        double destinationDistance = currentPoint[0].distance(destPoint);

        Point2D UP = new Point2D(-1,0);
        Point2D DOWN = new Point2D(1,0);
        Point2D LEFT = new Point2D(0,-1);
        Point2D RIGHT = new Point2D(0,1);

        Point2D finalDestPoint = destPoint;
        Task travel = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                while (true) {
                    Point2D nowPoint = new Point2D((int)agentImageView.getX(), (int)agentImageView.getY());
                    int currentX = (int) nowPoint.x;
                    int currentY = (int) nowPoint.y;

                    System.out.println("NX:"+nowPoint.x+" NY:"+nowPoint.y);


                    Point2D NUP = new Point2D(currentX-1,currentY);
                    Point2D NDOWN = new Point2D(currentX+1,currentY);
                    Point2D NLEFT = new Point2D(currentX,currentY-1);
                    Point2D NRIGHT = new Point2D(currentX,currentY+1);


                    double distNUP = finalDestPoint.distance(NUP);
                    double distNDOWN = finalDestPoint.distance(NDOWN);
                    double distNLEFT = finalDestPoint.distance(NLEFT);
                    double distNRIGHT = finalDestPoint.distance(NRIGHT);

                    ArrayList<Double> distances = new ArrayList<>();
                    distances.add(distNUP);
                    distances.add(distNDOWN);
                    distances.add(distNLEFT);
                    distances.add(distNRIGHT);

                    //choose greatest reduction in distances moved
                    double maxMoved = Collections.min(distances);

                    if(maxMoved == distNUP){
                        agentImageView.setX(NUP.x);
                        agentImageView.setY(NUP.y);
                    }
                    else if (maxMoved == distNDOWN){
                        agentImageView.setX(NDOWN.x);
                        agentImageView.setY(NDOWN.y);
                    }
                    else if(maxMoved == distNLEFT){
                        agentImageView.setX(NLEFT.x);
                        agentImageView.setY(NLEFT.y);
                    }
                    else if(maxMoved == distNRIGHT){
                        agentImageView.setX(NRIGHT.x);
                        agentImageView.setY(NRIGHT.y);
                    } else {
                        System.out.println("NOTMOVING");
                    }

                    if (nowPoint.distance(finalDestPoint)<=0) {
                        System.out.println("BROKEN FROM TRAVEL");
                        updateSelfPosition();
                        break;
                    }
                    updateSelfPosition();
                    Thread.sleep(20);
                }

//                for (int i=0; i<100; i++) {
//                    agentImageView.relocate(agent_X+i, agent_Y);
//                    Thread.sleep(10);
//                }
                return null;
            }
        }; new Thread(travel).start();

    }

    private class MoveCar extends WakerBehaviour {

        public MoveCar(Agent a, long timeout) {
            super(a, timeout);
        }

        @Override
        protected void onWake() {
            super.onWake();
            travel();
        }
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

    private class TravelAround extends CyclicBehaviour {

        @Override
        public void action() {

        }
    }

    private class UpdatePositionList extends TickerBehaviour {

        public UpdatePositionList(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            listOfAgentPositions = mapsInstance.getAgentsMappedPoint2D();
        }
    }
}
