package almacen;

public class LogEntry {
    private int term;
    private String command; // Podrías guardar la operación CRUD

    public LogEntry(int term, String command) {
        this.term = term;
        this.command = command;
    }

    public int getTerm() {
        return term;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "LogEntry{term=" + term + ", command='" + command + "'}";
    }
}
