package syntax;

public sealed interface Java17Style permits Java17Implementation {
    String value();
}

final class Java17Implementation implements Java17Style {
    public String value() {
        return "ready";
    }
}
