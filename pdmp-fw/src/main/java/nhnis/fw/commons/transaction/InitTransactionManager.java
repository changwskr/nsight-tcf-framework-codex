package nhnis.fw.commons.transaction;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * <PRE>
 * 설명
 *
 * </PRE>
 *
 * @author 홍길동
 * @version 1.0, 2026. 7. 15.
 * @logicalName
 */
@Component
public class InitTransactionManager implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public static PlatformTransactionManager initTransactionManager(String transactionName) {
        PlatformTransactionManager transactionManager = (PlatformTransactionManager) applicationContext
                .getBean(transactionName);
        TransactionManagerHolder transactionManagerHolder = new TransactionManagerHolder(transactionManager);
        ServiceTransactionManager.setInstance(transactionManagerHolder);
        return transactionManager;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
