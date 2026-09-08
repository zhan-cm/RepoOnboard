package io.github.zhancm.repoonboard.core.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoreContractDependencyTest {

    @Test
    void publicRecordComponentsExposeOnlyJdkOrCoreModelTypes() {
        List<Class<?>> records = List.of(SourceLocation.class, Evidence.class, Diagnostic.class);

        for (Class<?> record : records) {
            for (RecordComponent component : record.getRecordComponents()) {
                String packageName = component.getType().getPackageName();
                assertTrue(
                        packageName.startsWith("java.")
                                || packageName.equals("io.github.zhancm.repoonboard.core.model"),
                        () -> record.getSimpleName() + "." + component.getName()
                                + " exposes " + component.getType().getName());
            }
        }
    }
}
