package hollowrealm.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

class FancyMenuPoseStackGuardTest {
    @AfterEach
    void cleanup() {
        FancyMenuPoseStackGuard.resetRenderLevel();
    }

    @Test
    void tracksNestedRenderLevelDepthAndRecoversFromExceptionsAndUnderflow() {
        FancyMenuPoseStackGuard.resetRenderLevel();
        assertFalse(FancyMenuPoseStackGuard.shouldSkipPoseStackTracking());
        assertEquals(0, FancyMenuPoseStackGuard.currentDepth());

        // Underflow when depth is 0 must stay clamped at 0
        FancyMenuPoseStackGuard.exitRenderLevel();
        assertEquals(0, FancyMenuPoseStackGuard.currentDepth());
        assertFalse(FancyMenuPoseStackGuard.shouldSkipPoseStackTracking());

        // Nested enter/exit (e.g. shadow pass / portal pass inside renderLevel)
        FancyMenuPoseStackGuard.enterRenderLevel();
        assertEquals(1, FancyMenuPoseStackGuard.currentDepth());
        assertTrue(FancyMenuPoseStackGuard.shouldSkipPoseStackTracking());

        FancyMenuPoseStackGuard.enterRenderLevel();
        assertEquals(2, FancyMenuPoseStackGuard.currentDepth());
        assertTrue(FancyMenuPoseStackGuard.shouldSkipPoseStackTracking());

        FancyMenuPoseStackGuard.exitRenderLevel();
        assertEquals(1, FancyMenuPoseStackGuard.currentDepth());
        assertTrue(FancyMenuPoseStackGuard.shouldSkipPoseStackTracking());

        FancyMenuPoseStackGuard.exitRenderLevel();
        assertEquals(0, FancyMenuPoseStackGuard.currentDepth());
        assertFalse(FancyMenuPoseStackGuard.shouldSkipPoseStackTracking());

        // Exception inside runInRenderLevel must always restore depth via try/finally
        assertThrows(IllegalStateException.class, () -> FancyMenuPoseStackGuard.runInRenderLevel(() -> {
            assertTrue(FancyMenuPoseStackGuard.shouldSkipPoseStackTracking());
            throw new IllegalStateException("simulated renderLevel failure");
        }));
        assertEquals(0, FancyMenuPoseStackGuard.currentDepth());
        assertFalse(FancyMenuPoseStackGuard.shouldSkipPoseStackTracking());
    }

    @Test
    void isolatesRenderThreadFromBackgroundWorkerThreads() throws InterruptedException {
        FancyMenuPoseStackGuard.resetRenderLevel();
        FancyMenuPoseStackGuard.enterRenderLevel();
        assertTrue(FancyMenuPoseStackGuard.shouldSkipPoseStackTracking());

        AtomicBoolean workerSawSkip = new AtomicBoolean(true);
        Thread worker = new Thread(() -> {
            workerSawSkip.set(FancyMenuPoseStackGuard.shouldSkipPoseStackTracking());
            // Worker exitRenderLevel must not decrement the render thread's active depth
            FancyMenuPoseStackGuard.exitRenderLevel();
        }, "hr-fixes-test-worker");
        worker.start();
        worker.join();

        assertFalse(workerSawSkip.get(), "Background worker thread must not inherit render-thread skip state");
        assertEquals(1, FancyMenuPoseStackGuard.currentDepth(), "Worker thread must not mutate render-thread depth");
        assertTrue(FancyMenuPoseStackGuard.shouldSkipPoseStackTracking());

        FancyMenuPoseStackGuard.exitRenderLevel();
        assertFalse(FancyMenuPoseStackGuard.shouldSkipPoseStackTracking());
    }

    @Test
    void executesTransformedPoseStackGuardMethodInJvmBytecode() throws Exception {
        byte[] rawBytes = buildSyntheticPoseStackHost();
        ClassNode node = new ClassNode();
        new ClassReader(rawBytes).accept(node, 0);

        assertTrue(HRFixesMixinPlugin.applyFancyMenuPoseStackGuard(node));

        ClassWriter cw = new ClassWriter(0);
        node.accept(cw);
        byte[] transformedBytes = cw.toByteArray();

        ClassLoader loader = new ClassLoader(FancyMenuPoseStackGuardTest.class.getClassLoader()) {
            Class<?> define(String name, byte[] bytes) {
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
        Class<?> hostClass = ((ClassLoader) loader).getClass()
                .getDeclaredMethod("define", String.class, byte[].class)
                .invoke(loader, "hollowrealm.performance.SyntheticPoseStackHost", transformedBytes) instanceof Class<?> c
                ? c : null;
        Object instance = hostClass.getDeclaredConstructor(boolean.class).newInstance(false);
        Method check = hostClass.getDeclaredMethod("isRenderSystemModelViewStack_FancyMenu");
        check.setAccessible(true);

        FancyMenuPoseStackGuard.resetRenderLevel();
        // Outside renderLevel: returns the underlying modelViewStack comparison (false)
        assertEquals(false, check.invoke(instance));

        // Inside renderLevel: guard short-circuits and returns true so FancyMenu skips tracking
        FancyMenuPoseStackGuard.runInRenderLevel(() -> {
            try {
                assertEquals(true, check.invoke(instance));
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        });

        // After renderLevel: returns false again for 2D Screen/GUI/HUD rendering
        assertEquals(false, check.invoke(instance));
    }

    private static byte[] buildSyntheticPoseStackHost() {
        String internalName = "hollowrealm/performance/SyntheticPoseStackHost";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "modelView", "Z", null, null).visitEnd();

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Z)V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitVarInsn(Opcodes.ILOAD, 1);
        init.visitFieldInsn(Opcodes.PUTFIELD, internalName, "modelView", "Z");
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PRIVATE, "isRenderSystemModelViewStack_FancyMenu", "()Z", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, internalName, "modelView", "Z");
        Label falseLabel = new Label();
        Label endLabel = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, falseLabel);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);
        mv.visitLabel(falseLabel);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitLabel(endLabel);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
