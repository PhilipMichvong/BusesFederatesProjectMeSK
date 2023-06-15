package hla13.hla13.Events;

import hla13.hla13.domain.Rozklad;

import java.util.ArrayList;

public class RozkladEvent {
    private ArrayList<Rozklad> schedule;

    public RozkladEvent(ArrayList<Rozklad> scheduleS){
        this.schedule = scheduleS;
    }

    public ArrayList<Rozklad> getSchedule() {return schedule;}
}
