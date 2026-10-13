import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Prints the real descriptor of TFMG's levelTick method so it can be compared with the string
 * hard-coded in the mixin's @Inject annotation (see verify-injection-target.sh).
 *
 * Usage: java -cp <tfmg jar>:<neoforge merged jar>:<event bus jar>:<this dir> VerifyInjectionTarget
 */
public final class VerifyInjectionTarget {

    public static void main(String[] args) throws Exception {
        ClassLoader cl = VerifyInjectionTarget.class.getClassLoader();
        Class<?> target = Class.forName("com.drmangotea.tfmg.base.events.TFMGCommonEvents", false, cl);
        Class<?> postEvent = Class.forName("net.neoforged.neoforge.event.tick.LevelTickEvent$Post", false, cl);

        Method method = target.getDeclaredMethod("levelTick", postEvent);
        String descriptor = method.getName()
                + MethodType.methodType(method.getReturnType(), method.getParameterTypes()).toMethodDescriptorString();

        System.out.println("DESCRIPTOR=" + descriptor);
        System.out.println("STATIC=" + Modifier.isStatic(method.getModifiers()));
    }

    private VerifyInjectionTarget() {
    }
}
