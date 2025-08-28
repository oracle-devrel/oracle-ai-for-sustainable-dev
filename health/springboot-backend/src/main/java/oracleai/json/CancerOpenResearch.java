package oracleai.json;

import lombok.Getter;
import lombok.Setter;

public class CancerOpenResearch {

    @Getter @Setter private int monthNumber;
    @Getter @Setter private String month;
    @Getter @Setter private int sumSevereLowerBound;
    @Getter @Setter private int sumSevereUpperBound;
    @Getter @Setter private int sumFatalityLowerBound;
    @Getter @Setter private int sumFatalityUpperBound;

    public CancerOpenResearch(int monthNumber, String month,
                              int sumSevereLowerBound, int sumSevereUpperBound,
                              int sumFatalityLowerBound, int sumFatalityUpperBound) {
        this.monthNumber = monthNumber;
        this.month = month;
        this.sumSevereLowerBound = sumSevereLowerBound;
        this.sumSevereUpperBound = sumSevereUpperBound;
        this.sumFatalityLowerBound = sumFatalityLowerBound;
        this.sumFatalityUpperBound = sumFatalityUpperBound;
    }
}
