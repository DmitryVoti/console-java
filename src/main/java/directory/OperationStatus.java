package directory;

public class OperationStatus {
    public enum Status {
        OK,
        ERROR
    }

    private final Status status;
    private final String message;

    public OperationStatus(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "[" + status + "] " + message;
    }
}