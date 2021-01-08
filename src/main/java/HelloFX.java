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
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Random;

public class HelloFX extends Application {

    //FLAGS
    private String PORT_NAME = "7778";
    private String HOSTNAME = "localhost";
    private double canvas_x = 640;
    private double canvas_y = 480;
    private int NumPowerAgents = 5;
    private int NumPowerDisAgents = 10;

    //VARS
    private String SoSAgentContainerName = "SoSAgentContainer";
    private ContainerController powerAgentContainerController;
    private String powerAgentContainerName  = "PowerAgentContainer";
    private HashMap<String, ImageView> powAgentMap = new HashMap<String, ImageView>();

    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Canvas canvas = new Canvas(canvas_x, canvas_y);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        Image ig = new Image(getClass().getResource("gen.png").toExternalForm());
        Image ig1 = new Image(getClass().getResource("power_storage.png").toExternalForm());

        Runtime runtime = Runtime.instance();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Runnable updater = new Runnable() {
                  @Override
                  public void run() {
                      startPowerAgents(runtime, NumPowerAgents, ig, root);
                      startSoSAgent(runtime);
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

        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (int i=1; i<NumPowerDisAgents; i++) {
                    startPowerDistributionAgents(powerAgentContainerController, i, ig1, root);
                    renderImage(ig1, root, new Random().nextInt((int)canvas_x), new Random().nextInt((int)canvas_y), "");
                }
                return null;
            }
        }; new Thread(task).start();

        root.getChildren().addAll(canvas);
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        try {
            powerAgentContainerController.kill();
            //should probably implement a cleaner way
            powerAgentContainerController.getPlatformController().kill();
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
        String agentName = "SoSAgent";
        try {
            AgentController ag = containerController.createNewAgent(agentName, "SoS.SoSAgent",
                    new Object[]{});
            ag.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    private void startPowerAgents(Runtime runtime, int agentNum, Image ig, Group g) {
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.CONTAINER_NAME, powerAgentContainerName);
        profile.setParameter(Profile.MAIN_HOST, HOSTNAME);
        profile.setParameter(Profile.MAIN_PORT, PORT_NAME);
        ContainerController containerController = runtime.createAgentContainer(profile);
        powerAgentContainerController = containerController;

        for(int i=1; i<=agentNum; i++) {
            String agentName = "PowerGenAgent_" + String.valueOf(i);
            try {
                AgentController ag = containerController.createNewAgent(agentName,
                        "power.PowerGenAgent",
                        new Object[]{});//arguments
                ag.start();
                renderImage(ig, g, new Random().nextInt((int)canvas_x), new Random().nextInt((int)canvas_y), agentName);
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }
    }

    private void startPowerDistributionAgents(ContainerController cc, int agentNum, Image ig, Group g) {
        if (cc != null) {
            String agentName = "PowerStoreDisAgent_" + String.valueOf(agentNum);
            try {
                AgentController ag = cc.createNewAgent(agentName,
                        "power.PowerStoreDisAgent",
                        new Object[]{});//arguments
                ag.start();
                renderImage(ig, g, new Random().nextInt((int)canvas_x), new Random().nextInt((int)canvas_y), agentName);
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }

        }
    }

    private void renderImage(Image ig, Group g, double x, double y, String agentName) {
        ImageView iv = new ImageView(ig);
        iv.setFitHeight(25);
        iv.setFitWidth(25);
        iv.setX(x);
        iv.setY(y);
        powAgentMap.put(agentName, iv);
        g.getChildren().addAll(iv);
    }

    public static void main(String[] args) {
        launch();
    }

}