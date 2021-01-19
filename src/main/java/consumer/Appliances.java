package consumer;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;

import java.util.*;

public class Appliances {
    private int sample;
    private String[] applianceNames;
    private Double[] applianceProbability;
    List<Pair<String, Double>> list = new ArrayList<>();
    HashMap<String, Double> applianceElecProfile = new HashMap<>();
    HashMap<String, Double> allAppliances = new HashMap<>();

    public Appliances(int sample) {
        //values obtained from:
        //Mansouri, I., Newborough, M., & Probert, D. (1996). Energy consumption in uk households: Impact of domestic electrical appliances. Applied Energy, 54(3 SPEC. ISS.), 211–285. https://doi.org/10.1016/0306-2619(96)00001-3
        //page 251(book) or 41/75 (article)
        //applianceNames = new String[] {"WashingMachine", "AirConditioner", "MicrowaveOven", "VacuumCleaner", "PersonalComputer", "MassageChair", "ClothesDryer", "Television"};
       // applianceProbability = new Double[] {96.1, 79.7, 62.1, 74.4, 81.4, 8.3, 19.2, 97.0};
        this.sample = sample;
        applianceNames = new String[] {"Refrigeration", "Dishwasher", "Lights", "Television", "WashingMachine", "ElectricOven", "MicrowaveOven", "VacuumCleaner"};
        applianceProbability = new Double[] {177.0, 43.0, 100.0, 160.0, 93.0, 56.0, 74.0, 100.0};

        applianceElecProfile.put("Refrigeration", new Random().doubles(calculateHourly(300), calculateHourly(1700)).findFirst().getAsDouble());
        applianceElecProfile.put("Dishwasher", new Random().doubles(calculateHourly(360), calculateHourly(730)).findFirst().getAsDouble());
        applianceElecProfile.put("Lights", new Random().doubles(calculateHourly(200), calculateHourly(1200)).findFirst().getAsDouble());
        applianceElecProfile.put("Television", new Random().doubles(calculateHourly(50), calculateHourly(800)).findFirst().getAsDouble());
        applianceElecProfile.put("WashingMachine", new Random().doubles(calculateHourly(40), calculateHourly(1300)).findFirst().getAsDouble());
        applianceElecProfile.put("ElectricOven", new Random().doubles(calculateHourly(60), calculateHourly(600)).findFirst().getAsDouble());
        applianceElecProfile.put("MicrowaveOven", new Random().doubles(calculateHourly(40), calculateHourly(150)).findFirst().getAsDouble());
        applianceElecProfile.put("VacuumCleaner", new Random().doubles(calculateHourly(30), calculateHourly(200)).findFirst().getAsDouble());

        for (int i=0; i<applianceProbability.length; i++) {
            list.add(new Pair(applianceNames[i], applianceProbability[i]));
        }
    }

    public HashMap<String, Double> initAppliances() {
        EnumeratedDistribution enumeratedDistribution = new EnumeratedDistribution(list);
        Object[] objectsList = enumeratedDistribution.sample(sample);
        Set<Object> set = new HashSet<>();
        for (int i=0; i<objectsList.length; i++) {
            //adds only unique object
            set.add(objectsList[i]);
        }

        objectsList = set.toArray();

        for (int i=0; i<objectsList.length; i++) {
            String applianceName = objectsList[i].toString();
            allAppliances.put(applianceName, applianceElecProfile.get(applianceName));
        }

        return allAppliances;
    }

    public HashMap<String, Double> getAppliances() {
        return allAppliances;
    }

    private double calculateHourly(double value) {
        value = ((value/12)/30)/24;
        return value;
    }
}
