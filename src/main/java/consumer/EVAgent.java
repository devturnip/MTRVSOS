package consumer;

import com.sun.javafx.geom.Point2D;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Maps;
import utils.Utils;

import java.util.*;

public class EVAgent extends Agent {
    private double agent_X = 0;
    private double agent_Y = 0;
    private ImageView agentImageView;
    private double canvas_x = 1024;
    private double canvas_y = 768;
    private boolean isTravelling = false;
    private int moveDistance = 20;
    private int moveDistanceStatic = 20;
    private double maxCapacity = 0;
    private double holdCapacity = 0;
    private double consumptionRate = 0;

    private Maps mapsInstance = Maps.getMapsInstance();
    private Utils utility = new Utils();

    private LinkedHashMap<AID, Double> nearestNeighbours = new LinkedHashMap<>();
    private Map.Entry<AID, Double> nearestNeighbour = null;
    private HashMap<String, Point2D> listOfAgentPositions = new HashMap<>();

    //message sending flags
    private AID currentNeighbour;
    private AID nextNeighbour;

    //logs
    private static Logger LOGGER = LoggerFactory.getLogger(EVAgent.class);

    //logs

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

        In here, every unit of pixel moved should equal 0.1km.

        The below shows the different capacity models and their consumption rate.
        kwh/ km (Dixon, J., & Bell, K. (2020).)
        24 kWh: 0.17 kWh/km
        64 kWh: 0.16 kWh/km
        100 kWh: 0.24 kWh/km

        Thus for this simulator,for each EVAgent, its consumption rate will be a range from:
        0.17-0.24

        Others:
        JavaFX uses reverse cartesian coordinates system; 0,0 means upper left corner.
        As Y values increases, point moves downwards from top.
         */
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        System.out.println(getLocalName() + " takedown. Killing...");
        mapsInstance.removeUI(agentImageView);
        doDelete();
    }

    private void initCapacity() {
        /*
        EV capacities sourced from:
        Dixon, J., & Bell, K. (2020).
        Electric vehicles: Battery capacity, charger power, access to charging and the impacts on distribution networks.
        ETransportation, 4, 100059. https://doi.org/10.1016/j.etran.2020.100059
         */
        //capacities are in kwh: for example 24kwh means in 24 kw of power is used in 1h.
        List<Integer> carCapacities = Arrays.asList(24, 22, 16, 40, 64, 60, 45, 77);
        //capacity chosen from uniform distribution of capacities.
        maxCapacity = carCapacities.get(new Random().nextInt(carCapacities.size()));
        holdCapacity = maxCapacity; //all cars start with full charge
        consumptionRate = (new Random().doubles(0.17, 0.24).findFirst().getAsDouble())/1000;
        LOGGER.info("MAXCAPACITY OF "+getLocalName() + ": " + maxCapacity + "kwh");
    }

    private void initPosition() {
        HashMap<String, ImageView> hm = mapsInstance.getAgentMap(this.getLocalName(), true);
        HashMap.Entry<String, ImageView> entry = hm.entrySet().iterator().next();
        String agentName = entry.getKey();
        ImageView iv = entry.getValue();
        agentImageView = iv;
        agent_X = iv.getX();
        agent_Y = iv.getY();
        LOGGER.debug("THIS:" + this.getLocalName() + " agent:" + agentName + " X:" + agent_X + " Y:" + agent_Y);
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
                LOGGER.debug("DEST:"+x0+":"+y0);
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
                    //calculate remaining battery first
                    double percent = (holdCapacity/maxCapacity)*100;
                    LOGGER.info(getLocalName()+" at "+ percent + ". Hold:" + holdCapacity + " Max:" + maxCapacity);
                    double toDeduct = consumptionRate * moveDistance;
                    boolean stopCharging = false;

                    //updateSelfPosition();
                    Point2D nowPoint = new Point2D((int)agentImageView.getX(), (int)agentImageView.getY());
                    int currentX = (int) nowPoint.x;
                    int currentY = (int) nowPoint.y;

                    LOGGER.debug(getLocalName()+" NX:"+nowPoint.x+" NY:"+nowPoint.y + " MOVEDIST:" + moveDistance);

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

                    if (percent >= 20 && stopCharging == false) {
                        if (maxMoved == distNUP) {
                            isTravelling = true;
                            LOGGER.debug(getLocalName() + ":UP");
                            holdCapacity = holdCapacity-toDeduct;
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    agentImageView.setX(NUP.x);
                                    agentImageView.setY(NUP.y);
                                }
                            });
                        }
                        else if (maxMoved == distNDOWN) {
                            isTravelling = true;
                            LOGGER.debug(getLocalName() + ":DOWN");
                            holdCapacity = holdCapacity-toDeduct;
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    agentImageView.setX(NDOWN.x);
                                    agentImageView.setY(NDOWN.y);
                                }
                            });

                        }
                        else if (maxMoved == distNLEFT) {
                            isTravelling = true;
                            LOGGER.debug(getLocalName() + ":LEFT");
                            holdCapacity = holdCapacity-toDeduct;
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    agentImageView.setX(NLEFT.x);
                                    agentImageView.setY(NLEFT.y);
                                }
                            });
                        }
                        else if (maxMoved == distNRIGHT) {
                            isTravelling = true;
                            LOGGER.debug(getLocalName() + ":RIGHT");
                            holdCapacity = holdCapacity-toDeduct;
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    agentImageView.setX(NRIGHT.x);
                                    agentImageView.setY(NRIGHT.y);

                                }
                            });

                        }
                        else {
                            //isTravelling = false;
                            LOGGER.debug("NOTMOVING");
                            break;
                        }

                        if (nowPoint.distance(finalDestPoint) <= 0) {
                            isTravelling = false;
                            LOGGER.debug("BROKEN FROM TRAVEL");
                            break;
                        }
                        else if (nowPoint.distance(finalDestPoint) <= moveDistanceStatic && nowPoint.distance(finalDestPoint) > 5) {
                            moveDistance = 5;
                        }
                        else if (nowPoint.distance(finalDestPoint) <= 5 && nowPoint.distance(finalDestPoint) > 0) {
                            moveDistance = 1;
                        }
                        else {
                            moveDistance = moveDistanceStatic;
                        }
                        Thread.sleep(50);
                    }
                    else {
                        stopCharging = true;
                        //find nearest charging station
                    }
                }

                return null;
            }
        }; new Thread(travel).start();
    }

    private void chargeCar() {

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
            initCapacity();
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
