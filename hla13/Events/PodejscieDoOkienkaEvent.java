package hla13.hla13.Events;

public class PodejscieDoOkienkaEvent {
    private int passengerNumber;
    private int time;

    public PodejscieDoOkienkaEvent(int passengerNumber, int time){
        this.passengerNumber = passengerNumber;
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    public int getPassengerNumber() {
        return passengerNumber;
    }
}
