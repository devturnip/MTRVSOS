import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;

import java.awt.*;

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
        renderImage(ig, root);

        root.getChildren().addAll(canvas);

        stage.setScene(new Scene(root));
        stage.show();

    }

    private void renderImage(Image ig, Group g) {
        ImageView iv = new ImageView(ig);
        iv.setFitHeight(20);
        iv.setFitWidth(20);
        iv.setX(50);
        iv.setY(25);
        g.getChildren().addAll(iv);
    }

    public static void main(String[] args) {
        launch();
    }

}