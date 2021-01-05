import jade.core.Agent;
import javafx.application.Application;

public class HelloWorldAgent extends Agent{
    private HelloFX helloFX;
    protected void setup() {
        System.out.println("Hello world! I'm an agent!");
        System.out.println("My local name is " + getAID().getLocalName());
        System.out.println("My GUID is " + getAID().getName());
        System.out.println("My addresses are " + String.join(",", getAID().getAddressesArray()));
        Application.launch(HelloFX.class);
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }
}
