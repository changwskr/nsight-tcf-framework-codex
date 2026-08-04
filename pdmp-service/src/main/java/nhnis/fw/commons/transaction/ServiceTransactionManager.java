package nhnis.fw.commons.transaction;

public class ServiceTransactionManager {

    private static ThreadLocal<TransactionManagerHolder> transactionManager = new ThreadLocal<>();

    public static void setInstance(TransactionManagerHolder holder) {
        transactionManager.set(holder);
    }

    public static TransactionManagerHolder getInstance() {
        return transactionManager.get();
    }

    public static void remove() {
        TransactionManagerHolder txHolder = transactionManager.get();
        if (txHolder != null) {
            txHolder.release();
        }
        transactionManager.remove();
    }
}
