package com.pms.hotel;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifies the module boundaries declared by package structure (root package
 * = public API, {@code .internal} = hidden implementation) are actually
 * respected - i.e. no module reaches into another module's internals, and
 * the module dependency graph has no cycles.
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(PmsModulithApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentationSnapshot() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
