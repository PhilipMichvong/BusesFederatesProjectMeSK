package hla13.hla13.Events;

import hla13.hla13.domain.Rozklad;

public class OdjazdEvent {
    private Rozklad schedule;

    public OdjazdEvent(int godzina, int linia){
        schedule = new Rozklad(godzina,linia);
    }

   public Rozklad getSchedule() {
        return schedule;
    }
}
