package hla13.hla13.domain;

public class Autobus {
    public int id;
    public Rozklad schedule;
    public int emptyPlaces;

    public Autobus(int id, Rozklad schedule, int emptyPlaces){
        this.id = id;
        this.schedule = schedule;
        this.emptyPlaces = emptyPlaces;
    }

    public Rozklad getSchedule() {
        return schedule;
    }

    public int getEmptyPlaces() {
        return emptyPlaces;
    }

    public void setEmptyPlaces(int emptyPlaces) {
        this.emptyPlaces = emptyPlaces;
    }

    public int getId() {
        return id;
    }
}
