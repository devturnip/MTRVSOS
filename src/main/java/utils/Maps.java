package utils;

import com.sun.javafx.geom.Point2D;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Maps {
    private static Maps mapInstance = new Maps();
    private HashMap<String, ImageView> agentMap = new HashMap<>();
    private HashMap<String, Label>  agentLabelMap = new HashMap<>();
    private HashMap<String, Point2D> agentMapPoint2D = new HashMap<>();
    private Group group = null;
    private static Logger LOGGER = LoggerFactory.getLogger(Maps.class);
    private HashMap<String, Label> demandGenLabel = new HashMap<>();

    private Maps(){}

    public static Maps getMapsInstance() { return mapInstance;}

    public void setGroup(Group group) {
        this.group = group;
    }

    public Group getGroup(){
        if (group!=null) {
            return group;
        } else {
            return null;
        }
    }

    public void removeUI(Object iv) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (group!=null) {
                    group.getChildren().remove(iv);
                }
            }
        });
    }

    public void mapAgentLocation (String agentName, ImageView ig) {
        //System.out.println(agentName + "XY:" + ig.getX() + "," + ig.getY());
        agentMap.put(agentName, ig);
    }

    public void mapAgentLabel (String agentName, Label label){
        agentLabelMap.put(agentName, label);
    }

    public HashMap<String, ImageView> getAgentsMappedLocation() {
        return agentMap;
    }

    public void mapAgentLocation (String agentName, Point2D point2D) {
        agentMapPoint2D.put(agentName, point2D);
    }

    public HashMap<String, Point2D> getAgentsMappedPoint2D() {
        return agentMapPoint2D;
    }

//    public HashMap<String, ImageView> getAgentMap(String agentName, boolean single) {
//        HashMap<String, ImageView> retSubMap = new HashMap<>();
//        HashMap<String, ImageView> agentMap = this.agentMap;
//
//        if(!single) {
//            for (String key : agentMap.keySet()) {
//                if (key.contains(agentName)) {
//                    retSubMap.put(key, agentMap.get(key));
//                }
//            }
//        } else if (single) {
//            for (String key : agentMap.keySet()) { //concurrent modification exception
//                if (key.equals(agentName)) {
//                    retSubMap.put(key, agentMap.get(key));
//                }
//            }
//        }
//        return retSubMap;
//    }

    public HashMap<String, ImageView> getAgentMap(String agentName, boolean single) {
        HashMap<String, ImageView> retSubMap = new HashMap<>();
        HashMap<String, ImageView> agentMap = new HashMap<>(this.agentMap);

        if(!single) {
            for (String key : agentMap.keySet()) {
                if (key.contains(agentName)) {
                    retSubMap.put(key, agentMap.get(key));
                }
            }
        } else if (single) {
            for (String key : agentMap.keySet()) {
                /*
                concurrent modification exception
                this happens because agents are not fully loaded into the map,
                and agentmap is being accessed at the same time by initposition() methods in each agent.
                more time needs to be given based on no. of agents, or at least init position needs to be called only after ALL agents are loaded.
                 */
                if (key.equals(agentName)) {
                    retSubMap.put(key, agentMap.get(key));
                }
            }
        }
        return retSubMap;
    }

    public HashMap<String, Label> getAgentLabelMap(String agentName) {
        HashMap<String, Label> retSubMap = new HashMap<>();
        HashMap<String, Label> agentLabelMap = this.agentLabelMap;
        for (String key : agentLabelMap.keySet()) {
                if (key.equals(agentName)) {
                    retSubMap.put(key, agentLabelMap.get(key));
                }
            }
        return retSubMap;
    }

    public void mapDemandGenLabel(String labelName, Label label) { demandGenLabel.put(labelName, label); }

    public HashMap<String, Label> getDemandGenLabel() { return demandGenLabel; }

    public void changeColor(ImageView iv, String Colour) throws InterruptedException {
        final Color[] color = new Color[1];
        Lighting lighting = new Lighting();
        lighting.setDiffuseConstant(1.0);
        lighting.setSpecularConstant(1.0);
        lighting.setSpecularExponent(1.0);
        lighting.setSurfaceScale(1.0);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                switch (Colour){
                    case "RED":
                        color[0] = Color.RED;
                        break;
                    case "GREEN":
                        color[0] = Color.GREEN;
                        break;
                    case "BLUE":
                        color[0] = Color.BLUE;
                        break;
                    case "LIGHTBLUE":
                        color[0] = Color.LIGHTBLUE;
                        break;
                    case "ORANGE":
                        color[0] = Color.ORANGE;
                        break;
                    case "YELLOWGREEN":
                        color[0] = Color.YELLOWGREEN;
                        break;
                    default:
                        color[0] = Color.TRANSPARENT;
                        break;
                }

                lighting.setLight(new Light.Distant(45, 45, color[0]));
                iv.setEffect(lighting);
            }
        });
    }
}
