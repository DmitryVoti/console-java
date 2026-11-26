package directory;

import lombok.NonNull;

public record OperationResult(@NonNull Status status, @NonNull String message) {

    public enum Status {
        OK,
        ERROR
    }

    @Override
    public String toString() {
        return "[%s] %s".formatted(status, message);
    }
}