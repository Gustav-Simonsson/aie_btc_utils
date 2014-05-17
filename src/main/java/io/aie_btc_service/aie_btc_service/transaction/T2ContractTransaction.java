package io.aie_btc_service.aie_btc_service.transaction;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.script.Script;
import io.aie_btc_service.aie_btc_service.OpenOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class T2ContractTransaction {

    public static final Logger slf4jLogger =
            LoggerFactory.getLogger(T2ContractTransaction.class);

    private final long id;

    private final Transaction transaction;
    private final OpenOutput firstOpenOutput;
    private final OpenOutput secondOpenOutput;
    private TransactionInput firstTransactionInput;
    private TransactionInput secondTransactionInput;

    public static final Transaction.SigHash sigHashAll = Transaction.SigHash.ALL;

    public T2ContractTransaction(Transaction transaction, OpenOutput firstOpenOutput, OpenOutput secondOpenOutput) {
        this.transaction = transaction;
        this.firstOpenOutput = firstOpenOutput;
        this.secondOpenOutput = secondOpenOutput;
        this.id = generateUniqueId();
    }

    public long getContractId() {
        return this.id;
    }

    public void setFirstTransactionInput(TransactionInput firstTransactionInput) {
        this.firstTransactionInput = firstTransactionInput;
    }

    public void setSecondTransactionInput(TransactionInput secondTransactionInput) {
        this.secondTransactionInput = secondTransactionInput;
    }

    public Sha256Hash getHashForFirstInputTransaction() {
        int inputCount = getCountOfInputs();
        transaction.addInput(firstTransactionInput);

        return transaction.hashForSignature(inputCount, new Script(firstOpenOutput.hash), sigHashAll, true);
    }

    public Sha256Hash getHashForSecondInputTransaction() {
        int inputCount = getCountOfInputs();
        transaction.addInput(secondTransactionInput);

        return transaction.hashForSignature(inputCount, new Script(secondOpenOutput.hash), sigHashAll, true);
    }

    public Transaction getTransaction() {
        return transaction;
    }

    private int getCountOfInputs() {
        int outputCount    = transaction.getOutputs().size();
        int inputCount     = transaction.getInputs().size();
        boolean firstInput = inputCount == 0;
        if (inputCount > 1) {
            // TODO: design proper exception classes
            throw new RuntimeException("More than one input in T2 step 2: " + firstInput);
        }
        if (outputCount != 1) {
            throw new RuntimeException("T2 does not have single output in T2 step 2: " + outputCount);
        }
        return inputCount;
    }

    private long generateUniqueId() {
        return 1l;
    }

    public String toString() {
        return "";
    }
}
