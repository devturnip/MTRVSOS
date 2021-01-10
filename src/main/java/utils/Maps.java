package utils;

import javafx.scene.image.ImageView;

import java.util.HashMap;

public class Maps {
    private static Maps mapInstance = new Maps();
    private HashMap<String, ImageView> agentMap = new HashMap<String, ImageView>();
    private Maps(){}

    public static Maps getMapsInstance() { return mapInstance;}

    public void MapAgentLocation (String agentName, ImageView ig) {
        //System.out.println(agentName + "XY:" + ig.getX() + "," + ig.getY());
        agentMap.put(agentName, ig);
    }

    public HashMap<String, ImageView> getAgentMap(String agentName, boolean single) {

        HashMap<String, ImageView> retSubMap = new HashMap<>();
        HashMap<String, ImageView> agentMap = this.agentMap;

        //agentMap.forEach((k,v) -> System.out.println(k + ":" + v));

//        if(!single) {
//            for (String key : agentMap.keySet()) {
//                if (key.contains(agentName)) {
//                    retSubMap.put(key, agentMap.get(key));
//                }
//            }
//        } else if (single) {
//            System.out.println("SINGLE");
//            for (String key : agentMap.keySet()) {
//
//                if (key.equals(agentName)) {
//                    retSubMap.put(key, agentMap.get(key));
//                }
//            }
//        }
        //return retSubMap;
        return agentMap;
    }
}
