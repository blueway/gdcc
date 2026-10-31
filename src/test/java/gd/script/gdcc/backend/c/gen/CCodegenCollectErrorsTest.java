package gd.script.gdcc.backend.c.gen;

import gd.script.gdcc.backend.CodegenContext;
import gd.script.gdcc.backend.ProjectInfo;
import gd.script.gdcc.enums.GodotVersion;
import gd.script.gdcc.gdextension.ExtensionApiLoader;
import gd.script.gdcc.lir.LirBasicBlock;
import gd.script.gdcc.lir.LirClassDef;
import gd.script.gdcc.lir.LirFunctionDef;
import gd.script.gdcc.lir.LirModule;
import gd.script.gdcc.lir.insn.LoadPropertyInsn;
import gd.script.gdcc.lir.insn.ReturnInsn;
import gd.script.gdcc.scope.ClassRegistry;
import gd.script.gdcc.type.GdVoidType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CCodegenCollectErrorsTest {
    @Test
    void collectModeReportsEveryFailingFunctionInOneSummary() throws Exception {
        var hostClass = new LirClassDef("CollectErrorsHost", "RefCounted", false, false,
                Map.of(), List.of(), List.of(), List.of());
        hostClass.setSourceFile("collect_errors_host.gd");
        hostClass.addFunction(invalidPropertyLoad("first_failure"));
        hostClass.addFunction(invalidPropertyLoad("second_failure"));

        var module = new LirModule("collect_errors_module", List.of(hostClass));
        var classRegistry = new ClassRegistry(ExtensionApiLoader.loadDefault());
        var projectInfo = new ProjectInfo("CollectErrorsProject", GodotVersion.V451, Path.of(".")) {
        };
        var codegen = new CCodegen();
        codegen.setCollectCodegenErrors(true);
        codegen.prepare(new CodegenContext(projectInfo, classRegistry), module);

        var ex = assertThrows(IllegalStateException.class, codegen::generate);
        assertTrue(ex.getMessage().contains("collected 2 error(s)"), ex.getMessage());
        assertTrue(ex.getMessage().contains("first_failure"), ex.getMessage());
        assertTrue(ex.getMessage().contains("second_failure"), ex.getMessage());
    }

    private static @NotNull LirFunctionDef invalidPropertyLoad(@NotNull String name) {
        var function = new LirFunctionDef(name);
        function.setReturnType(GdVoidType.VOID);
        var entry = new LirBasicBlock("entry");
        entry.appendInstruction(new LoadPropertyInsn("result", "missing", "missing_receiver"));
        entry.appendInstruction(new ReturnInsn(null));
        function.addBasicBlock(entry);
        function.setEntryBlockId("entry");
        return function;
    }
}
