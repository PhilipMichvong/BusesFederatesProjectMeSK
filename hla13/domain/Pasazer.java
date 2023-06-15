package hla13.hla13.domain;

public class Pasazer {
    public int id;
    public Rozklad rozklad;
    public String status = null;
    public double czasZmiany = 0;

    public Pasazer(int id, int godzina, int linia ){
        rozklad = new Rozklad(godzina, linia);
        this.id = id;
    }
}
