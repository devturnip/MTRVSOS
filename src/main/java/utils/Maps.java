package utils;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import java.util.HashMap;

public class Maps {
    private static Maps mapInstance = new Maps();
    private HashMap<String, ImageView> agentMap = new HashMap<String, ImageView>();
    private Group group = null;

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

    public HashMap<String, ImageView> getAgentsMappedLocation() {
        return agentMap;
    }

    public HashMap<String, ImageView> getAgentMap(String agentName, boolean single) {
        HashMap<String, ImageView> retSubMap = new HashMap<>();
        HashMap<String, ImageView> agentMap = this.agentMap;

        if(!single) {
            for (String key : agentMap.keySet()) {
                if (key.contains(agentName)) {
                    retSubMap.put(key, agentMap.get(key));
                }
            }
        } else if (single) {
            for (String key : agentMap.keySet()) {
                if (key.equals(agentName)) {
                    retSubMap.put(key, agentMap.get(key));
                }
            }
        }
        return retSubMap;
    }

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
