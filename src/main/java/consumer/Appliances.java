package consumer;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class Appliances {
    private String name;
    private double powerCost;
    private int sample;
    private String[] applianceNames;
    private Double[] applianceProbability;
    List<Pair<String, Double>> list = new ArrayList<Pair<String, Double>>();

    public Appliances(int sample) {
        //source Singapore Department of Statistics (2018)
        //Subject/Topic: Households with Consumer Durables/Services
        this.sample = sample;
        applianceNames = new String[] {"WashingMachine", "AirConditioner", "MicrowaveOven", "VacuumCleaner", "PersonalComputer", "MassageChair", "ClothesDryer", "Television"};
        applianceProbability = new Double[] {96.1, 79.7, 62.1, 74.4, 81.4, 8.3, 19.2, 97.0};
        for (int i=0; i<applianceProbability.length; i++) {
            list.add(new Pair(applianceNames[i], applianceProbability[i]));
        }
    }

    public Object[] getAppliances() {
        EnumeratedDistribution enumeratedDistribution = new EnumeratedDistribution(list);
        return enumeratedDistribution.sample(sample);
    }
}
