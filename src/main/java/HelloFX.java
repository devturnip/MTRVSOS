import com.sun.javafx.geom.Point2D;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import power.Power;
import utils.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class HelloFX extends Application {

    Settings settingsInstance = Settings.getSettingsInstance();

    //FLAGS
    private String PORT_NAME = settingsInstance.getPORT_NAME();
    private String HOSTNAME = settingsInstance.getHOSTNAME();
    private int multiplier = settingsInstance.getMultiplier();
    private int imageHeightXY = settingsInstance.getImageHeightXY();
    private int homeImageXY = settingsInstance.getHomeImageXY();
    private int evImageXY = settingsInstance.getEvImageXY();
    private double canvas_x = settingsInstance.getCanvasX();
    private double canvas_y = settingsInstance.getCanvasY();
    private int numPowerAgents = settingsInstance.getNumPowerAgents();
    private int numPowerDisAgents = settingsInstance.getNumPowerDisAgents();
    private int numSmartHomeAgents = settingsInstance.getNumSmartHomeAgents();
    private int numEVAgents = settingsInstance.getNumEVAgents();
    private int secondsToRun = settingsInstance.getSecondsToRun();

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
    private String sosAgentName = "SoSAgent";
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
                            totalPowerLevels = powerInstance.getGridMax();
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
        progressBar.setPrefSize(400,15);
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

        Label genRate = new Label("G (kwh/s): 0");
        Label demandRate = new Label("D (kwh/s): 0");
        Label utilisationRate = new Label("G/D: 0%");

        mapsInstance.mapDemandGenLabel("genRate", genRate);
        mapsInstance.mapDemandGenLabel("demandRate", demandRate);

        Thread updateGD = new Thread(() -> {
            while (true) {
                Runnable updater = () -> {
                    double demandRate1 = powerInstance.getDemand();
                    double genRate1 = powerInstance.getGenRate();
                    //LOGGER.debug("GENRATE: "+ String.valueOf(genRate1));
                    double gdRate = (demandRate1 / genRate1) * 100;
                    BigDecimal bigDecimal1 = new BigDecimal(genRate1).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal bigDecimal2 = new BigDecimal(demandRate1).setScale(2, RoundingMode.HALF_UP);
                    genRate.setText("G (kwh/s): " + bigDecimal1.doubleValue());
                    demandRate.setText("D (kwh/s): " + bigDecimal2.doubleValue());
                    if (Double.isFinite(gdRate)) {
                        BigDecimal bigDecimal = new BigDecimal(gdRate).setScale(2, RoundingMode.HALF_UP);
                        utilisationRate.setText("D/G: " + bigDecimal.doubleValue() + "%");
                    } else {
                        utilisationRate.setText("-%");
                    }
                };
                Platform.runLater(updater);
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        updateGD.start();


        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15,12,15,12));
        hbox.setSpacing(10);
        hbox.setStyle("-fx-background-color: #B0C4DE;");
        hbox.setAlignment(Pos.BASELINE_LEFT);
        hbox.getChildren().addAll(label, progressvalues, genRate, demandRate, utilisationRate);

        VBox vBox = new VBox();
        Button pauseButton = new Button();
        pauseButton.setText("Pause");
        pauseButton.setMinWidth(vBox.getPrefWidth());
        pauseButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                settingsInstance.setPauseSimulation(true);
            }
        });

        Button playButton = new Button();
        playButton.setText("Play");
        playButton.setMinWidth(vBox.getPrefWidth());
        playButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                settingsInstance.setPauseSimulation(false);
            }
        });

        Button exitButton = new Button();
        exitButton.setText("Exit");
        exitButton.setMinWidth(vBox.getPrefWidth());
        exitButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    Platform.exit();
                    System.exit(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        vBox.getChildren().addAll(playButton, pauseButton, exitButton);
        vBox.setPadding(new Insets(5,10,10,10));
        vBox.setAlignment(Pos.BOTTOM_CENTER);
        vBox.setStyle("-fx-background-color: #E6E6FA;");

        BorderPane border = new BorderPane();
        border.setCenter(canvas);
        border.setRight(vBox);
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
                      if (settingsInstance.getTestRun().equalsIgnoreCase("consistentreliabilitythreshold")) {
                          startTestAgent();
                      }
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
                ArrayList<Rectangle2D> pointsBox = new ArrayList<>();

                while (locationMap.hasNext()) {
                    Map.Entry<String, Point2D> m = (Map.Entry<String, Point2D>) locationMap.next();
                    Point2D point2DVals = m.getValue();
                    points.add(point2DVals);
                    Rectangle2D rectanglePoints = new Rectangle2D(point2DVals.x, point2DVals.y, imageHeightXY*multiplier, imageHeightXY*multiplier);
                    pointsBox.add(rectanglePoints);
                }

                while ((agentName = agentsQueue.take())!=null && !agentName.equals("")) {
                    int x=0;
                    int y=0;

                    while (true) {
                        //collision prevention (doesnt work)
                        int x0 = new Random().ints(imageHeightXY*multiplier, ((int)canvas_x-(imageHeightXY*multiplier))).findFirst().getAsInt();
                        int y0 = new Random().ints(imageHeightXY*multiplier, ((int)canvas_y-(imageHeightXY*multiplier))).findFirst().getAsInt();

                        if (agentName.contains("SmartHomeAgent")) {
                            x0 = new Random().ints(homeImageXY*multiplier, ((int)canvas_x-(homeImageXY*multiplier))).findFirst().getAsInt();
                            y0 = new Random().ints(homeImageXY*multiplier, ((int)canvas_y-(homeImageXY*multiplier))).findFirst().getAsInt();
                        }
                        else if (agentName.contains("EVAgent")){
                            x0 = new Random().ints(evImageXY*multiplier, ((int)canvas_x-(evImageXY*multiplier))).findFirst().getAsInt();
                            y0 = new Random().ints(evImageXY*2*multiplier, ((int)canvas_y-(evImageXY*2*multiplier))).findFirst().getAsInt();
                        }

                        Point2D point2D2Compare = new Point2D(x0,y0);
                        if(!points.contains(point2D2Compare)) {
                            x = (int) point2D2Compare.x;
                            y = (int) point2D2Compare.y;
                            boolean toBreak = false;
                            ArrayList<Rectangle2D> intersect = new ArrayList<>();
                            Rectangle2D rectangle2DToCompare = new Rectangle2D(x, y, imageHeightXY*multiplier, imageHeightXY*multiplier);

                            if(agentName.contains("SmartHomeAgent")) {
                                rectangle2DToCompare = new Rectangle2D(x,y, homeImageXY*multiplier, homeImageXY*multiplier);
                            }
                            else if (agentName.contains("EVAgent")) {
                                rectangle2DToCompare = new Rectangle2D(x,y, evImageXY*multiplier, evImageXY*1.75*multiplier);
                            }
                            for (Rectangle2D rect:pointsBox) {
                                if (rect.intersects(rectangle2DToCompare)){
                                    //add intersecting points; we don't really need this,
                                    //we break the loop only when intersect is empty
                                    //thus avoiding collision (as much as possible) when spawning agents.
                                    intersect.add(rectangle2DToCompare);
                                }
                            }
                            if(intersect.isEmpty()) {
                                break;
                            }
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

        TimeTracker timeTracker = TimeTracker.getTimeTrackerInstance();
        timeTracker.startClock(secondsToRun);

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
            evAgentContainerController.kill();
            evAgentContainerController.getPlatformController().kill();
            Runtime.instance().shutDown();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private void startSoSAgent(Runtime runtime) {
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.CONTAINER_NAME, SoSAgentContainerName);
        profile.setParameter(Profile.MAIN_HOST, HOSTNAME);
        profile.setParameter(Profile.MAIN_PORT, PORT_NAME);
        ContainerController containerController = runtime.createAgentContainer(profile);
        sosAgentContainerController = containerController;
        String agentName = sosAgentName;
        try {
            AgentController ag = containerController.createNewAgent(agentName, "SoS.SoSAgent",
                    new Object[]{});
            ag.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    private void startTestAgent() {
        String agentName = "rand-test-agent";
        try {
            AgentController ag = sosAgentContainerController.createNewAgent(agentName, "metamorphic.TestAgent",
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

    private void renderContextMenu(ImageView imageView, String agentName){
        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItem = new MenuItem("Kill");
        contextMenu.getItems().addAll(menuItem);

        imageView.setPickOnBounds(true);
        imageView.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent contextMenuEvent) {
                contextMenu.show(imageView, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
            }
        });

        menuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    if (agentName.contains("Power")){
                        AgentController agentController = powerAgentContainerController.getAgent(agentName);
                        agentController.kill();
                    }
                    else if (agentName.contains("SmartHomeAgent")) {
                        AgentController agentController = smartHomeAgentContainerController.getAgent(agentName);
                        agentController.kill();
                    }
                    else if(agentName.contains("EVAgent")) {
                        AgentController agentController = evAgentContainerController.getAgent(agentName);
                        agentController.kill();
                    }
                } catch (ControllerException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void renderImage(Group g, double x, double y, String agentName) {
        Label label = new Label(agentName);
        label.setMinSize(0.5,0.5);
        label.setFont(new Font(10));
        Image ig = new Image(getClass().getResource("gen.png").toExternalForm());
        Image ig1 = new Image(getClass().getResource("power_storage.png").toExternalForm());
        Image ig2 = new Image(getClass().getResource("smart_home.png").toExternalForm());
        Image ig3 = new Image(getClass().getResource("ev.png").toExternalForm());
        ImageView iv = null;
        if (agentName.contains("PowerGenAgent")) {
            iv = new ImageView(ig);
            iv.setFitHeight(imageHeightXY*multiplier);
            iv.setFitWidth(imageHeightXY*multiplier);
            label.setTranslateX(x-((imageHeightXY*multiplier)/3));
            label.setTranslateY(y+(imageHeightXY*multiplier));
        } else if (agentName.contains("PowerStoreDisAgent")) {
            iv = new ImageView(ig1);
            iv.setFitHeight(imageHeightXY*multiplier);
            iv.setFitWidth(imageHeightXY*multiplier);
            label.setTranslateX(x-((imageHeightXY*multiplier)/2));
            label.setTranslateY(y+(imageHeightXY*multiplier));
        } else if (agentName.contains("SmartHomeAgent")) {
            iv = new ImageView(ig2);
            iv.setFitHeight(homeImageXY*multiplier);
            iv.setFitWidth(homeImageXY*multiplier);
            label.setTranslateX(x-((homeImageXY*multiplier)/2));
            label.setTranslateY(y+(homeImageXY*multiplier));
        } else if (agentName.contains("EVAgent")) {
            iv = new ImageView(ig3);
            iv.setFitHeight(evImageXY*multiplier);
            iv.setFitWidth(evImageXY*1.75*multiplier);
            label.setTranslateX(x);
            label.setTranslateY(y+(evImageXY*multiplier));
        }
        iv.setX(x);
        iv.setY(y);
        renderContextMenu(iv, agentName);
        Point2D point2D = new Point2D((int)x,(int)y);
        mapsInstance.mapAgentLocation(agentName, point2D);
        //powAgentMap.put(agentName, iv);
        mapsInstance.mapAgentLocation(agentName, iv);
        mapsInstance.mapAgentLabel(agentName, label);
        g.getChildren().addAll(iv, label);
    }


    public static void main (String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new UXE());
        launch();
    }

}