package oracleai.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import oracleai.geojson.Feature;
import oracleai.geojson.FeatureCollection;

import java.util.List;

public class AnnualDeathCause {

/**
    annual_death_cause
    ENTITY                                       VARCHAR2(50)
    CODE                                         VARCHAR2(50)
    YEAR                                         NUMBER
    DEATHS_MENINGITIS                            NUMBER
    DEATHS_ALZHEIMERS_DISEASE                    NUMBER
    DEATHS_PARKINSON_DISEASE                     NUMBER
    DEATHS_NUTRITIONAL_DEFICIENCIES              NUMBER
    DEATHS_MALARIA                               NUMBER
    DEATHS_MATERNAL_DISORDERS                    NUMBER
*/

    @Getter @Setter private String entity;
    @Getter @Setter private int year;
    @Getter @Setter private int deathMeningitis;
    @Getter @Setter private int deathAlzheimersDisease;
    @Getter @Setter private int deathParkinsonDisease;
    @Getter @Setter private int deathNutritionalDeficiencies;
    @Getter @Setter private int deathMalaria;
    @Getter @Setter private int deathMaternalDisorders;

    public AnnualDeathCause(String entity, int year,
                            int deathMeningitis, int deathAlzheimersDisease, int deathParkinsonDisease,
                            int deathNutritionalDeficiencies, int deathMalaria, int deathMaternalDisorders) {
        this.entity = entity;
        this.year = year;
        this.deathMeningitis = deathMeningitis;
        this.deathAlzheimersDisease = deathAlzheimersDisease;
        this.deathParkinsonDisease = deathParkinsonDisease;
        this.deathNutritionalDeficiencies = deathNutritionalDeficiencies;
        this.deathMalaria = deathMalaria;
        this.deathMaternalDisorders = deathMaternalDisorders;
    }

}
