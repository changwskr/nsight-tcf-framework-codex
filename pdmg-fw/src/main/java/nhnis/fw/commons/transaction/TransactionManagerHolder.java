package nhnis.fw.commons.transaction;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class TransactionManagerHolder {

    private final Map<String, TransactionDefinition> definitionMap = new HashMap<>();
    private final Deque<TransactionStatus> txStack = new LinkedList<>();
    private final PlatformTransactionManager transactionManager;

    private static final String DEFAULT_DEFINITION = "defaultDefinition";

    public TransactionManagerHolder(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public PlatformTransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    public TransactionStatus beginTransaction() {
        TransactionStatus status = beginTransaction(DEFAULT_DEFINITION);
        return status;
    }

    public TransactionStatus beginTransaction(String definitionName) {
        TransactionDefinition txDefinition = this.definitionMap.get(definitionName);
        if (txDefinition == null) {
            return beginTransaction(definitionName,
                    TransactionDefinition.PROPAGATION_REQUIRES_NEW,
                    TransactionDefinition.ISOLATION_DEFAULT);
        } else {
            return beginTransaction(definitionName);
        }
    }

    public TransactionStatus beginTransaction(String definitionName, int propagationStrategy, int isolationLevel) {
        TransactionDefinition txDefinition = getDefaultTransactionDefinition(definitionName,
                propagationStrategy,
                isolationLevel);
        return beginTransaction(txDefinition);
    }

    public TransactionStatus beginTransaction(TransactionDefinition txDefinition) {
        TransactionStatus txStatus = this.transactionManager.getTransaction(txDefinition);
        this.txStack.add(txStatus);
        return txStatus;
    }

    private TransactionDefinition getDefaultTransactionDefinition(String definitionName, int propagationStrategy,
            int isolationLevel) {
        int isoLevel = -1;
        DefaultTransactionDefinition definition = (DefaultTransactionDefinition) this.definitionMap
                .get(definitionName);
        if (definition == null) {
            definition = new DefaultTransactionDefinition();
            definition.setName(definitionName);
            definition.setPropagationBehavior(propagationStrategy);
            if (isolationLevel == 0) {
                definition.setIsolationLevel(isoLevel);
            } else {
                definition.setIsolationLevel(isolationLevel);
            }
            definition.setIsolationLevel(isoLevel);
            this.definitionMap.putIfAbsent(definitionName, definition);
            definition = (DefaultTransactionDefinition) this.definitionMap.get(definitionName);
        }
        return (TransactionDefinition) definition;
    }

    public void commit() {
        TransactionStatus currentTx = this.txStack.pop();
        this.transactionManager.commit(currentTx);
    }

    public void commit(TransactionStatus txStatus) {
        this.transactionManager.commit(txStatus);
        this.txStack.remove(txStatus);
    }

    public void rollback() {
        TransactionStatus currentTx = this.txStack.pop();
        this.transactionManager.rollback(currentTx);
    }

    public void rollback(TransactionStatus txStatus) {
        this.transactionManager.rollback(txStatus);
        this.txStack.remove(txStatus);
    }

    public void release() {
        this.definitionMap.clear();
        this.txStack.clear();
    }
}
