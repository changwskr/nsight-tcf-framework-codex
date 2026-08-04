package nhnis.fw.commons.context;

public class ServiceContextHolder {

    private static ThreadLocal<ServiceContext> INSTANCE = new ThreadLocal<>();

    public static void setInstance(ServiceContext serviceContext) {
        INSTANCE.set(serviceContext);
    }

    public static ServiceContext getInstance() {
        return INSTANCE.get();
    }

    public static void removeInstance() {
        INSTANCE.remove();
    }
}
