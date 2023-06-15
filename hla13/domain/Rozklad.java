package hla13.hla13.domain;

public class Rozklad {
    public int time;
    public int line;

    public Rozklad(int time, int line){
        this.time = time;
        this.line = line;
    }

    @Override
    public boolean equals(Object obj){
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Rozklad other = (Rozklad) obj;
        if (!(time == (other.time)))
            return false;
        if (!(line == other.line))
            return false;
        return true;
    }
}
