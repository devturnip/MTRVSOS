package consumer;

import com.sun.javafx.geom.Point2D;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import javafx.application.Platform;
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
    private boolean isTravelling = false;
    private int moveDistance = 50;
    private int moveDistanceStatic = 50;


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
        addBehaviour(new MoveCar(this, 5000));

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

        /*
        Point2D UP = new Point2D(-1,0);
        Point2D DOWN = new Point2D(1,0);
        Point2D LEFT = new Point2D(0,-1);
        Point2D RIGHT = new Point2D(0,1);
        */

        Point2D finalDestPoint = destPoint;
        Task travel = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                while (true) {
                    //updateSelfPosition();
                    Point2D nowPoint = new Point2D((int)agentImageView.getX(), (int)agentImageView.getY());
                    int currentX = (int) nowPoint.x;
                    int currentY = (int) nowPoint.y;

                    System.out.println(getLocalName()+" NX:"+nowPoint.x+" NY:"+nowPoint.y + " MOVEDIST:" + moveDistance);


                    Point2D NUP = new Point2D(currentX-moveDistance,currentY);
                    Point2D NDOWN = new Point2D(currentX+moveDistance,currentY);
                    Point2D NLEFT = new Point2D(currentX,currentY-moveDistance);
                    Point2D NRIGHT = new Point2D(currentX,currentY+moveDistance);


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
                        isTravelling = true;
                        System.out.println(getLocalName()+":UP");
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                agentImageView.setX(NUP.x);
                                agentImageView.setY(NUP.y);
                            }
                        });
                    }
                    else if (maxMoved == distNDOWN){
                        isTravelling = true;
                        System.out.println(getLocalName()+":DOWN");
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                agentImageView.setX(NDOWN.x);
                                agentImageView.setY(NDOWN.y);
                            }
                        });

                    }
                    else if(maxMoved == distNLEFT){
                        isTravelling = true;
                        System.out.println(getLocalName()+":LEFT");
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                agentImageView.setX(NLEFT.x);
                                agentImageView.setY(NLEFT.y);
                            }
                        });
                    }
                    else if(maxMoved == distNRIGHT){
                        isTravelling = true;
                        System.out.println(getLocalName()+":RIGHT");
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                agentImageView.setX(NRIGHT.x);
                                agentImageView.setY(NRIGHT.y);

                            }
                        });

                    } else {
                        //isTravelling = false;
                        System.out.println("NOTMOVING");
                        break;
                    }

                    if (nowPoint.distance(finalDestPoint)<=0) {
                        isTravelling = false;
                        System.out.println("BROKEN FROM TRAVEL");
                        break;
                    } else if (nowPoint.distance(finalDestPoint)<=50 && nowPoint.distance(finalDestPoint)>5){
                        moveDistance = 5;
                    } else if (nowPoint.distance(finalDestPoint)<=5 && nowPoint.distance(finalDestPoint)>0) {
                        moveDistance = 1;
                    } else {
                        moveDistance = moveDistanceStatic;
                    }
                    Thread.sleep(50);
                }

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
            addBehaviour(new TravelAround(myAgent, 2000));
            //travel();
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

   private class TravelAround extends TickerBehaviour {

       public TravelAround(Agent a, long period) {
           super(a, period);
       }

       @Override
       protected void onTick() {
           if (isTravelling == false) {
               travel();
           } else {
               //updateSelfPosition();
               block();
           }
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
