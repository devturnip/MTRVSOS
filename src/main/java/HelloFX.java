import com.sun.javafx.geom.Point2D;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import utils.Maps;
import utils.Utils;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class HelloFX extends Application {

    //FLAGS
    private String PORT_NAME = "7778";
    private String HOSTNAME = "localhost";
    private int multiplier = 2;
    private int imageHeightXY = 30;
    private int homeImageXY = 20;
    private int evImageXY = 15;
    private double canvas_x = 1280;
    private double canvas_y = 1020;
    private int numPowerAgents = 3;
    private int numPowerDisAgents = 3;
    private int numSmartHomeAgents = 5;
    private int numEVAgents = 1;

    //VARS
    private String SoSAgentContainerName = "SoSAgentContainer";
    private String powerAgentContainerName  = "PowerAgentContainer";
    private String smartHomeAgentContainerName  = "SmartHomeAgentContainer";
    private String evAgentContainerName = "EVAgentContainer";
    private ContainerController powerAgentContainerController;
    private ContainerController smartHomeAgentContainerController;
    private ContainerController sosAgentContainerController;
    private ContainerController evAgentContainerController;
    private HashMap<String, ImageView> powAgentMap = new HashMap<String, ImageView>();

    private BlockingQueue<String> agentsQueue = new LinkedBlockingQueue<>();
    private Utils utility = new Utils();
    private Maps mapsInstance = Maps.getMapsInstance();

    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Canvas canvas = new Canvas(canvas_x, canvas_y);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.GRAY);



        Runtime runtime = Runtime.instance();

        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                for (int i=1; i<=numPowerAgents; i++) {
                    String agentName = startPowerAgents(powerAgentContainerController, i);
                    agentsQueue.put(agentName);
                }
                for (int i=1; i<=numPowerDisAgents; i++) {
                    String agentName = startPowerDistributionAgents(powerAgentContainerController, i);
                    agentsQueue.put(agentName);
                }
                for (int i=1; i<=numSmartHomeAgents; i++) {
                    String agentName = startSmartHomeAgent(smartHomeAgentContainerController, i);
                    agentsQueue.put(agentName);
                }
                for (int i=1; i<=numEVAgents; i++) {
                    String agentName = startEVAgent(evAgentContainerController, i);
                    agentsQueue.put(agentName);
                }
                return null;
            }
        };

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Runnable updater = new Runnable() {
                  @Override
                  public void run() {
                      startPowerContainer(runtime);
                      startSmartHomeContainer(runtime);
                      startEVContainer(runtime);
                      startSoSAgent(runtime);
                      new Thread(task).start();
                  }
                };
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException ex) {
                }
                Platform.runLater(updater);
            }
        });
        thread.start();

        Task render = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String agentName = "";
                HashMap<String, ImageView> allAgentsMap = mapsInstance.getAgentsMappedLocation();
                Iterator locationMap = allAgentsMap.entrySet().iterator();
                ArrayList<Point2D> points = new ArrayList<>();

                while (locationMap.hasNext()) {
                    Map.Entry<String, ImageView> m = (Map.Entry<String, ImageView>) locationMap.next();
                    int x = (int) m.getValue().getX();
                    int y = (int) m.getValue().getY();
                    points.add(new Point2D(x,y));
                }

                while ((agentName = agentsQueue.take())!=null && !agentName.equals("")) {
                    int x=0;
                    int y=0;

                    while (true) {
                        //collision prevention
                        int x0 = new Random().ints(imageHeightXY*multiplier, ((int)canvas_x-(imageHeightXY*multiplier))).findFirst().getAsInt();
                        int y0 = new Random().ints(imageHeightXY*multiplier, ((int)canvas_y-(imageHeightXY*multiplier))).findFirst().getAsInt();
                        Point2D point2D2Compare = new Point2D(x0,y0);
                        if(!points.contains(point2D2Compare)) {
                            x = (int) point2D2Compare.x;
                            y = (int) point2D2Compare.y;
                            break;
                        }
                    }

                    String finalAgentName = agentName;
                    int finalX = x;
                    int finalY = y;
                    Platform.runLater(new Runnable() {
                        public void run() {
                            renderImage(root, finalX, finalY, finalAgentName);
                        }
                    });

                }
                return null;
            }
        }; new Thread(render).start();

        root.getChildren().addAll(canvas);
        mapsInstance.setGroup(root);
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        try {
            powerAgentContainerController.kill();
            smartHomeAgentContainerController.kill();
            powerAgentContainerController.getPlatformController().kill();
            smartHomeAgentContainerController.getPlatformController().kill();
            sosAgentContainerController.kill();
            sosAgentContainerController.getPlatformController().kill();
            Runtime.instance().shutDown();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void startSoSAgent(Runtime runtime) {
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.CONTAINER_NAME, SoSAgentContainerName);
        profile.setParameter(Profile.MAIN_HOST, HOSTNAME);
        profile.setParameter(Profile.MAIN_PORT, PORT_NAME);
        ContainerController containerController = runtime.createAgentContainer(profile);
        sosAgentContainerController = containerController;
        String agentName = "SoSAgent";
        try {
            AgentController ag = containerController.createNewAgent(agentName, "SoS.SoSAgent",
                    new Object[]{});
            ag.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    private void startEVContainer(Runtime runtime) {
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.CONTAINER_NAME, evAgentContainerName);
        profile.setParameter(Profile.MAIN_HOST, HOSTNAME);
        profile.setParameter(Profile.MAIN_PORT, PORT_NAME);
        ContainerController containerController = runtime.createAgentContainer(profile);
        evAgentContainerController = containerController;
    }

    private void startSmartHomeContainer(Runtime runtime) {
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.CONTAINER_NAME, smartHomeAgentContainerName);
        profile.setParameter(Profile.MAIN_HOST, HOSTNAME);
        profile.setParameter(Profile.MAIN_PORT, PORT_NAME);
        ContainerController containerController = runtime.createAgentContainer(profile);
        smartHomeAgentContainerController = containerController;
    }

    private String startEVAgent(ContainerController cc, int agentNum) {
        String agentName = "";
        if (cc != null) {
            agentName = "EVAgent_" + String.valueOf(agentNum);
            try {
                AgentController ag = cc.createNewAgent(agentName, "consumer.EVAgent",
                        new Object[]{});
                ag.start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
            return agentName;
        }
        return  agentName;
    }

    private String startSmartHomeAgent(ContainerController cc, int agentNum) {
        String agentName = "";
        if (cc != null) {
            agentName = "SmartHomeAgent_" + String.valueOf(agentNum);
            try {
                AgentController ag = cc.createNewAgent(agentName, "consumer.SmartHomeAgent",
                        new Object[]{});
                ag.start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
            return agentName;
        }
        return  agentName;
    }

    private void startPowerContainer(Runtime runtime) {
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.CONTAINER_NAME, powerAgentContainerName);
        profile.setParameter(Profile.MAIN_HOST, HOSTNAME);
        profile.setParameter(Profile.MAIN_PORT, PORT_NAME);
        ContainerController containerController = runtime.createAgentContainer(profile);
        powerAgentContainerController = containerController;
    }

    private String startPowerAgents(ContainerController cc, int agentNum) {
        String agentName = "";
        if (cc != null) {
            agentName = "PowerGenAgent_" + String.valueOf(agentNum);
            try {
                AgentController ag = cc.createNewAgent(agentName,
                        "power.PowerGenAgent",
                        new Object[]{});//arguments
                ag.start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
            return agentName;
        }
        return agentName;
    }

    private String startPowerDistributionAgents(ContainerController cc, int agentNum) {
        String agentName = "";
        if (cc != null) {
            agentName = "PowerStoreDisAgent_" + String.valueOf(agentNum);
            try {
                AgentController ag = cc.createNewAgent(agentName,
                        "power.PowerStoreDisAgent",
                        new Object[]{});//arguments
                ag.start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
            return agentName;
        }
        return agentName;
    }

    private void renderImage(Group g, double x, double y, String agentName) {
        Image ig = new Image(getClass().getResource("gen.png").toExternalForm());
        Image ig1 = new Image(getClass().getResource("power_storage.png").toExternalForm());
        Image ig2 = new Image(getClass().getResource("smart_home.png").toExternalForm());
        Image ig3 = new Image(getClass().getResource("ev.png").toExternalForm());
        ImageView iv = null;
        if (agentName.contains("PowerGenAgent")) {
            iv = new ImageView(ig);
            iv.setFitHeight(imageHeightXY*multiplier);
            iv.setFitWidth(imageHeightXY*multiplier);
        } else if (agentName.contains("PowerStoreDisAgent")) {
            iv = new ImageView(ig1);
            iv.setFitHeight(imageHeightXY*multiplier);
            iv.setFitWidth(imageHeightXY*multiplier);
        } else if (agentName.contains("SmartHomeAgent")) {
            iv = new ImageView(ig2);
            iv.setFitHeight(homeImageXY*multiplier);
            iv.setFitWidth(homeImageXY*multiplier);
        } else if (agentName.contains("EVAgent")) {
            iv = new ImageView(ig3);
            iv.setFitHeight(evImageXY*multiplier);
            iv.setFitWidth(evImageXY*1.75*multiplier);
        }
        iv.setX(x);
        iv.setY(y);
        powAgentMap.put(agentName, iv);
        mapsInstance.mapAgentLocation(agentName, iv);
        g.getChildren().addAll(iv);
    }


    public static void main(String[] args) {
        launch();
    }

}