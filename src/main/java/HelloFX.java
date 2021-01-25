import com.sun.javafx.geom.Point2D;
import consumer.EVAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import power.Power;
import utils.Maps;
import utils.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private double canvas_x = 1024;
    private double canvas_y = 768;
    private int numPowerAgents = 1;
    private int numPowerDisAgents = 1;
    private int numSmartHomeAgents = 1;
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
    private double totalPowerLevels = 0;
    //private HashMap<String, ImageView> powAgentMap = new HashMap<String, ImageView>();

    private BlockingQueue<String> agentsQueue = new LinkedBlockingQueue<>();
    private Utils utility = new Utils();
    private Maps mapsInstance = Maps.getMapsInstance();
    private Power powerInstance = Power.getPowerInstance();

    //logs
    private static Logger LOGGER = LoggerFactory.getLogger(HelloFX.class);

    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Canvas canvas = new Canvas(canvas_x, canvas_y);

        Label progressvalues = new Label();
        progressvalues.setTextFill(Color.web("#8A2BE2"));
        final Task updatePowerLevels = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (true) {
                    totalPowerLevels = powerInstance.getGridMax();
                    if (totalPowerLevels!=0) {
                        LOGGER.debug("broken from loop for gridmax");
                        break;
                    }
                    Thread.sleep(1000);
                }
                for (int i=0; i<totalPowerLevels; i++) {
                    Platform.runLater(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            double currentActualPower = powerInstance.showPowerLevels();
                            double holder = (currentActualPower/totalPowerLevels)*100;
                            BigDecimal bd = new BigDecimal(holder).setScale(2, RoundingMode.HALF_UP);
                            BigDecimal current = new BigDecimal(currentActualPower).setScale(2, RoundingMode.HALF_UP);
                            BigDecimal max = new BigDecimal(totalPowerLevels).setScale(2, RoundingMode.HALF_UP);
                            double percentage = bd.doubleValue();
                            double currentPower = current.doubleValue();
                            double maxPower = max.doubleValue();
                            progressvalues.setText(current + " / "+maxPower+" ("+percentage+") %");
                        }
                    }));
                    updateProgress(i + powerInstance.showPowerLevels(), totalPowerLevels);
                    Thread.sleep(500);
                }
                return null;
            }
        };

        final ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefSize(600,15);
        Label label = new Label();
        label.setGraphic(progressBar);
        label.setText("Power Levels");
        label.setContentDisplay(ContentDisplay.LEFT);
        progressBar.progressProperty().bind(updatePowerLevels.progressProperty());

        progressBar.progressProperty().addListener(observable -> {
            if (totalPowerLevels > 0) {
                if (progressBar.getProgress() >= totalPowerLevels) {
                    progressBar.setStyle("-fx-accent: forestgreen;");
                } else if (progressBar.getProgress() <= totalPowerLevels) {
                    progressBar.setStyle("-fx-accent: lightskyblue;");
                }
            }
        });

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15,12,15,12));
        hbox.setSpacing(10);
        hbox.setStyle("-fx-background-color: #B0C4DE;");
        hbox.setAlignment(Pos.BASELINE_CENTER);
        hbox.getChildren().addAll(label, progressvalues);

        BorderPane border = new BorderPane();
        border.setCenter(canvas);
        border.setBottom(hbox);

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
                HashMap<String, Point2D> allAgentsMap = mapsInstance.getAgentsMappedPoint2D();
                Iterator locationMap = allAgentsMap.entrySet().iterator();
                ArrayList<Point2D> points = new ArrayList<>();

                while (locationMap.hasNext()) {
                    Map.Entry<String, Point2D> m = (Map.Entry<String, Point2D>) locationMap.next();
                    points.add(m.getValue());
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

        root.getChildren().addAll(border);
        mapsInstance.setGroup(root);
        stage.setScene(new Scene(root));
        stage.show();

        final Thread updateProgressBar = new Thread (updatePowerLevels, "updateProgressBar");
        updateProgressBar.setDaemon(true);
        updateProgressBar.start();
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
            e.printStackTrace();
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
        Point2D point2D = new Point2D((int)x,(int)y);
        mapsInstance.mapAgentLocation(agentName, point2D);
        //powAgentMap.put(agentName, iv);
        mapsInstance.mapAgentLocation(agentName, iv);
        g.getChildren().addAll(iv);
    }


    public static void main(String[] args) {
        launch();
    }

}