package syntax;

@Deprecated
public record Java21Style(String value) {
    public String describe(Object candidate) {
        return switch (candidate) {
            case String text when !text.isBlank() -> text;
            default -> value;
        };
    }
}
