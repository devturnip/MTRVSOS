import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import javafx.application.Application;

import java.util.Random;

public class HelloWorldAgent extends Agent{
    private HelloFX helloFX;
    protected void setup() {
        System.out.println("Hello world! I'm an agent!");
        System.out.println("My local name is " + getAID().getLocalName());
        System.out.println("My GUID is " + getAID().getName());
        System.out.println("My addresses are " + String.join(",", getAID().getAddressesArray()));
        /*Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.CONTAINER_NAME, "PowerGenAgentContainer");
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "7778");
        ContainerController containerController = runtime.createAgentContainer(profile);
        try {
            AgentController ag = containerController.createNewAgent("testagent",
                    "power.PowerGenAgent",
                    new Object[]{});//arguments
            ag.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }*/
        Application.launch(HelloFX.class);
    }
}
