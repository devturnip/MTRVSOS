import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;
import power.Power;
import power.PowerGenAgent;

import java.awt.*;
import java.util.Random;

public class HelloFX extends Application {

    private Image genImage;
    private ImageView genImageView;

    @Override
    public void start(Stage stage) {
       /* String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        StackPane root = new StackPane();
        Label lHello = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");


        root.getChildren().addAll(lHello,genImageView);

        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.show();*/

        Group root = new Group();
        Canvas canvas = new Canvas(640, 480);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Image ig = new Image(getClass().getResource("gen.png").toExternalForm());

        Runtime runtime = Runtime.instance();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Runnable updater = new Runnable() {
                  @Override
                  public void run() {
                      startPowerAgents(runtime, 5, ig, root);
                  }
                };
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                }
                Platform.runLater(updater);

            }
        });

        thread.start();
        //startPowerAgents(runtime, 5, ig, root);

        root.getChildren().addAll(canvas);
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void startPowerAgents(Runtime runtime, int agentNum, Image ig, Group g) {
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.CONTAINER_NAME, "PowerGenAgentContainer");
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "7778");
        ContainerController containerController = runtime.createAgentContainer(profile);

        for(int i=0; i<agentNum; i++) {
            String agentName = "PowerGenAgent_" + String.valueOf(i);
            try {
                AgentController ag = containerController.createNewAgent(agentName,
                        "power.PowerGenAgent",
                        new Object[]{});//arguments
                ag.start();
                renderImage(ig, g, new Random().nextInt(640), new Random().nextInt(480));
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }
    }

    private void renderImage(Image ig, Group g, double x, double y) {
        ImageView iv = new ImageView(ig);
        iv.setFitHeight(25);
        iv.setFitWidth(25);
        iv.setX(x);
        iv.setY(y);
        g.getChildren().addAll(iv);
    }

    public static void main(String[] args) {
        launch();
    }

}