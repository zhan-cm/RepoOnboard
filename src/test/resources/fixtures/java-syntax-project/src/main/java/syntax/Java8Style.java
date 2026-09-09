package syntax;

@Deprecated
public class Java8Style {
    @Deprecated
    private String name;

    @Deprecated
    public String name() {
        return name;
    }

    interface Nested {
    }
}

enum Lifecycle {
    NEW,
    READY
}
