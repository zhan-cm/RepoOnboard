package syntax;

public class Java11Style {
    public String repeat(String value) {
        var normalized = value.strip();
        return normalized.repeat(2);
    }
}
