package consumer;

import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.Rectangle;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Maps;
import utils.Settings;
import utils.Utils;

import java.util.*;

public class EVAgent extends Agent {
    Settings settingsInstance = Settings.getSettingsInstance();
    private double agent_X = 0;
    private double agent_Y = 0;
    private ImageView agentImageView;
    private Label agentLabel;
    private boolean isTravelling = false;
    private int moveDistance = 20;
    private int moveDistanceStatic = 20;
    private double maxCapacity = 0;
    private double holdCapacity = 0;
    private double consumptionRate = 0;
    private boolean stopCharging = false;
    private boolean tryNext = false;
    private boolean hasTarget = false;

    private double canvas_x = settingsInstance.getCanvasX();
    private double canvas_y = settingsInstance.getCanvasY();
    private int evImageXY = settingsInstance.getEvImageXY();
    private int multiplier = settingsInstance.getMultiplier();

    private Maps mapsInstance = Maps.getMapsInstance();
    private Utils utility = new Utils();

    private LinkedHashMap<AID, Double> nearestNeighbours = new LinkedHashMap<>();
    private Map.Entry<AID, Double> nearestNeighbour = null;
    private HashMap<String, Point2D> listOfAgentPositions = new HashMap<>();
    private HashMap<String, ImageView> listOfAgentImageViews = new HashMap<>();
    private ArrayList<Behaviour> behaviourList = new ArrayList<>();

    //colour flags
    private int currentColour = 0;
    private int GREEN = 1;
    private int ORANGE = 2;
    private int RED = 3;

    //message sending flags
    private AID currentNeighbour;
    private AID nextNeighbour;

    //logs
    private static Logger LOGGER = LoggerFactory.getLogger(EVAgent.class);

    @Override
    protected void setup() {
        super.setup();
        InitPosition initPosition = new InitPosition(this,2000);
        UpdatePositionList updatePositionList = new UpdatePositionList(this, 500);
        MoveCar moveCar = new MoveCar(this, 5000);
        behaviourList.add(initPosition);
        behaviourList.add(updatePositionList);
        behaviourList.add(moveCar);
        addBehaviour(initPosition);
        addBehaviour(updatePositionList);
        addBehaviour(moveCar);

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
        LOGGER.info(getLocalName() + " takedown. Killing...");
        mapsInstance.removeUI(agentImageView);
        if(!behaviourList.isEmpty()) {
            for (Behaviour b: behaviourList){
                LOGGER.info("Removing behaviour(s): "+b);
                removeBehaviour(b);
            }
        }
        mapsInstance.removeUI(agentImageView);
        mapsInstance.removeUI(agentLabel);
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
        consumptionRate = (new Random().doubles(0.17, 0.24).findFirst().getAsDouble())/10;
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

        HashMap<String, Label> lm = mapsInstance.getAgentLabelMap(this.getLocalName());
        Map.Entry<String, Label> labelEntry = lm.entrySet().iterator().next();
        agentLabel = labelEntry.getValue();
    }

    private void updateSelfPosition() {
        ImageView iv = agentImageView;
        int x_position = (int) iv.getX();
        int y_position = (int) iv.getY();
        Point2D updatedPoint2d = new Point2D(x_position, y_position);
        mapsInstance.mapAgentLocation(getLocalName(), updatedPoint2d);
    }

    private void travel(Agent agent) {
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
                    boolean breakLoop = false;
                    double percent = (holdCapacity / maxCapacity) * 100;
                    LOGGER.debug(getLocalName() + " at " + percent + ". Hold:" + holdCapacity + " Max:" + maxCapacity);
                    double toDeduct = consumptionRate * moveDistance;

                    Point2D nowPoint = new Point2D((int) agentImageView.getX(), (int) agentImageView.getY());
                    int currentX = (int) nowPoint.x;
                    int currentY = (int) nowPoint.y;

                    LOGGER.debug(getLocalName() + " NX:" + nowPoint.x + " NY:" + nowPoint.y + " MOVEDIST:" + moveDistance);

                    if (percent >= 30 && stopCharging==false) {
                        if (currentColour!=GREEN) {
                            currentColour = GREEN;
                            mapsInstance.changeColor(agentImageView, "GREEN");
                        }
                        nearestNeighbour = utility.getNearest(agent, currentX, currentY, "EV-Charging");
                        breakLoop = moveCar(agent, currentX, currentY, toDeduct, nowPoint, finalDestPoint, false, nearestNeighbour.getKey());

                    }
                    else{
                        if (currentColour != ORANGE) {
                            currentColour = ORANGE;
                            mapsInstance.changeColor(agentImageView, "ORANGE");
                        }
                        //find nearest charging station
                        if (tryNext==false) {
                            nearestNeighbour = utility.getNearest(agent, currentX, currentY, "EV-Charging");
                            HashMap<String, Point2D> agentPoints = mapsInstance.getAgentsMappedPoint2D();
                            Point2D nearestCharger = agentPoints.get(nearestNeighbour.getKey().getLocalName());
                            stopCharging = moveCar(agent, currentX, currentY, toDeduct, nowPoint, nearestCharger, true, nearestNeighbour.getKey());
                        } else {
                            if (hasTarget==false) {
                                nearestNeighbour = utility.getNearest(agent, currentX, currentY, "EV-Charging", nearestNeighbour.getKey());
                                LOGGER.debug("NEXT NEIGHBOUR:" + nearestNeighbour.getKey().getLocalName());
                                HashMap<String, Point2D> agentPoints = mapsInstance.getAgentsMappedPoint2D();
                                Point2D nearestCharger = agentPoints.get(nearestNeighbour.getKey().getLocalName());
                                stopCharging = moveCar(agent, currentX, currentY, toDeduct, nowPoint, nearestCharger, true, nearestNeighbour.getKey());
                                hasTarget=true;
                            } else {
                                HashMap<String, Point2D> agentPoints = mapsInstance.getAgentsMappedPoint2D();
                                Point2D nearestCharger = agentPoints.get(nearestNeighbour.getKey().getLocalName());
                                stopCharging = moveCar(agent, currentX, currentY, toDeduct, nowPoint, nearestCharger, true, nearestNeighbour.getKey());
                            }
                        }

                    }

                    if (breakLoop == true) {
                        break;
                    }

                    updateSelfPosition();
                }
                return null;
            }
        }; new Thread(travel).start();
    }

    private boolean moveCar(Agent agent, int currentX, int currentY, double toDeduct, Point2D nowPoint, Point2D destination, boolean toCharge, AID charger) throws InterruptedException {
        boolean breakLoop = false;

        Point2D NUP = new Point2D(currentX-moveDistance,currentY);
        Point2D NDOWN = new Point2D(currentX+moveDistance,currentY);
        Point2D NLEFT = new Point2D(currentX,currentY-moveDistance);
        Point2D NRIGHT = new Point2D(currentX,currentY+moveDistance);

        ArrayList<Point2D> steps = new ArrayList<>();
        steps.add(NUP);
        steps.add(NDOWN);
        steps.add(NLEFT);
        steps.add(NRIGHT);

        double distNUP = destination.distance(NUP);
        double distNDOWN = destination.distance(NDOWN);
        double distNLEFT = destination.distance(NLEFT);
        double distNRIGHT = destination.distance(NRIGHT);

        ArrayList<Double> distances = new ArrayList<>();
        distances.add(distNUP);
        distances.add(distNDOWN);
        distances.add(distNLEFT);
        distances.add(distNRIGHT);

//        //check for collision and remove collided moves: todo.... doesnt work...
//        for (int i=0; i<steps.size(); i++){
//            Iterator iterator = listOfAgentImageViews.entrySet().iterator();
//            Rectangle2D collisionBox = new Rectangle2D(steps.get(i).x, steps.get(i).y, 15*2,15*1.75*2);
//            if(iterator.hasNext()) {
//                Map.Entry pair = (Map.Entry) iterator.next();
//                ImageView imageView = (ImageView) pair.getValue();
//                Rectangle2D ivBox = new Rectangle2D(imageView.getX(), imageView.getY(), imageView.getBoundsInParent().getHeight(), imageView.getBoundsInParent().getWidth());
//                if(ivBox.intersects(collisionBox)){
//                    distances.remove(i);
//                }
//            }
//        }
        double maxMoved = Collections.min(distances);

        if (maxMoved == distNUP) {
            breakLoop = false;
            isTravelling = true;
            LOGGER.debug(getLocalName() + ":UP");
            holdCapacity = holdCapacity-toDeduct;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    agentImageView.setX(NUP.x);
                    agentImageView.setY(NUP.y);
                    agentLabel.setTranslateX(NUP.x);
                    agentLabel.setTranslateY(NUP.y+(evImageXY*multiplier));
                }
            });
        }
        else if (maxMoved == distNDOWN) {
            breakLoop = false;
            isTravelling = true;
            LOGGER.debug(getLocalName() + ":DOWN");
            holdCapacity = holdCapacity-toDeduct;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    agentImageView.setX(NDOWN.x);
                    agentImageView.setY(NDOWN.y);
                    agentLabel.setTranslateX(NDOWN.x);
                    agentLabel.setTranslateY(NDOWN.y+(evImageXY*multiplier));
                }
            });
        }
        else if (maxMoved == distNLEFT) {
            breakLoop = false;
            isTravelling = true;
            LOGGER.debug(getLocalName() + ":LEFT");
            holdCapacity = holdCapacity-toDeduct;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    agentImageView.setX(NLEFT.x);
                    agentImageView.setY(NLEFT.y);
                    agentLabel.setTranslateX(NLEFT.x);
                    agentLabel.setTranslateY(NLEFT.y+(evImageXY*multiplier));
                }
            });
        }
        else if (maxMoved == distNRIGHT) {
            breakLoop = false;
            isTravelling = true;
            LOGGER.debug(getLocalName() + ":RIGHT");
            holdCapacity = holdCapacity-toDeduct;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    agentImageView.setX(NRIGHT.x);
                    agentImageView.setY(NRIGHT.y);
                    agentLabel.setTranslateX(NRIGHT.x);
                    agentLabel.setTranslateY(NRIGHT.y+(evImageXY*multiplier));
                }
            });
        }
        else {
            //isTravelling = false;
            LOGGER.debug("NOTMOVING");
            breakLoop = true;
            if (currentColour!=RED) {
                currentColour = RED;
                mapsInstance.changeColor(agentImageView, "RED");
            }
        }

        if (nowPoint.distance(destination) <= 0 && toCharge == false) {
            isTravelling = false;
            LOGGER.debug("BROKEN FROM TRAVEL");
            breakLoop = true;

        }
        else if (nowPoint.distance(destination) <= moveDistanceStatic && nowPoint.distance(destination) > 5) {
            moveDistance = 5;
        }
        else if (nowPoint.distance(destination) <= 5 && nowPoint.distance(destination) > 0) {
            moveDistance = 1;
        }
        else {
            moveDistance = moveDistanceStatic;
        }

        if (toCharge == true && nowPoint.distance(destination) >=0 && nowPoint.distance(destination) <=4) {
            LOGGER.debug(getLocalName() + " is at charging stationnnnnnnnnnnnnnnnnnnn. Sending message to " + nearestNeighbour.getKey());
            HashMap.Entry<String, String> arguments = new HashMap.SimpleEntry<String, String>("toCharge", String.valueOf(maxCapacity));
            utility.sendMessageWithArgs(this, charger, arguments, "BEGIN_CHARGE", "REQUEST");
            double percent = (holdCapacity/maxCapacity) * 100;
            int rejectCount = 0;
            while (true) {
                ACLMessage msg = agent.receive();
                if (msg!=null) {
                    String contents = msg.getContent();
                    if (contents.equals("ACCEPT_CHARGE")) {
                        double chargeValue = Double.parseDouble(arguments.getValue());
                        double temp = holdCapacity + chargeValue;
                        if (temp>=maxCapacity) {
                            chargeValue = temp - maxCapacity;
                        }
                        holdCapacity = holdCapacity + chargeValue;
                        LOGGER.info(agent.getLocalName() + "fully charged: " + holdCapacity + "/" + maxCapacity);
                        if (percent > 10) {
                            tryNext = false;
                            hasTarget = false;
                            return true;
                        } else {
                            return false;
                        }
                    }
                    else if (contents.equals("REJECT_CHARGE")) {
                        if (rejectCount >= 10) {
                            rejectCount = 0;

                            if (percent > 10) {
                                tryNext = true;
                                return false;
                            }
                        }
                        rejectCount += 1;
                        LOGGER.info("Rejected charge");
                        Thread.sleep(1000);
                        utility.sendMessageWithArgs(this, charger, arguments, "BEGIN_CHARGE", "REQUEST");
                    }
                } else {
                    if (rejectCount >= 5) {
                        rejectCount = 0;

                        if (percent > 10) {
                            tryNext = true;
                            hasTarget = false;
                            return false;
                        }
                    }
                    rejectCount += 1;
                    LOGGER.debug(getLocalName() + " received no messages. COUNT=" + rejectCount);
                    Thread.sleep(1000);
                    utility.sendMessageWithArgs(this, charger, arguments, "BEGIN_CHARGE", "REQUEST");
                }

            }

        }
        Thread.sleep(50);

        return breakLoop;
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
               travel(myAgent);
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
            HashMap<String, ImageView> hm = mapsInstance.getAgentMap(getLocalName(), true);
            if (hm!=null && !hm.isEmpty()) {
                HashMap.Entry<String, ImageView> entry = hm.entrySet().iterator().next();
                String agentName = entry.getKey();
                ImageView iv = entry.getValue();
                agentImageView = iv;
                listOfAgentImageViews = mapsInstance.getAgentsMappedLocation();
                listOfAgentPositions = mapsInstance.getAgentsMappedPoint2D();
            }
        }
    }
}
