package hollowrealm.performance;

public final class FancyMenuPoseStackGuard {
    private static volatile Thread renderThread;
    private static volatile int renderLevelDepth;

    private FancyMenuPoseStackGuard() {
    }

    public static void enterRenderLevel() {
        Thread current = Thread.currentThread();
        if (renderThread != current) {
            renderThread = current;
            renderLevelDepth = 1;
        } else {
            renderLevelDepth++;
        }
    }

    public static void exitRenderLevel() {
        if (Thread.currentThread() == renderThread) {
            int next = renderLevelDepth - 1;
            renderLevelDepth = Math.max(0, next);
        }
    }

    public static void resetRenderLevel() {
        renderThread = Thread.currentThread();
        renderLevelDepth = 0;
    }

    public static boolean shouldSkipPoseStackTracking() {
        return renderLevelDepth > 0 && Thread.currentThread() == renderThread;
    }

    public static int currentDepth() {
        return Thread.currentThread() == renderThread ? renderLevelDepth : 0;
    }

    public static void runInRenderLevel(Runnable action) {
        enterRenderLevel();
        try {
            action.run();
        } finally {
            exitRenderLevel();
        }
    }
}
