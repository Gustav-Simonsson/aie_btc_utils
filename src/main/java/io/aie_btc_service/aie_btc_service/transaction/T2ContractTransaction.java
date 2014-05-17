package io.aie_btc_service.aie_btc_service.transaction;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.script.Script;
import io.aie_btc_service.aie_btc_service.OpenOutput;

public class T2ContractTransaction {

    private final Transaction transaction;

    private TransactionInput firstTransactionInput;
    private OpenOutput firstTransactionOpenOutput;

    private TransactionInput secondTransactionInput;
    private OpenOutput secondTransactionOpenOutput;

    public static final Transaction.SigHash sigHashAll = Transaction.SigHash.ALL;

    public T2ContractTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public void setFirstTransactionInput(TransactionInput firstTransactionInput, OpenOutput firstTransactionOpenOutput) {
        this.firstTransactionInput = firstTransactionInput;
        this.firstTransactionOpenOutput = firstTransactionOpenOutput;
    }

    public void setSecondTransactionInput(TransactionInput secondTransactionInput, OpenOutput secondTransactionOpenOutput) {
        this.secondTransactionInput = secondTransactionInput;
        this.secondTransactionOpenOutput = secondTransactionOpenOutput;
    }

    public Sha256Hash getHashForFirstInputTransaction() {
        int inputCount = getCountOfInputs();
        transaction.addInput(firstTransactionInput);

        return transaction.hashForSignature(inputCount, new Script(firstTransactionOpenOutput.hash), sigHashAll, true);
    }

    public Sha256Hash getHashForSecondInputTransaction() {
        int inputCount = getCountOfInputs();
        transaction.addInput(firstTransactionInput);

        return transaction.hashForSignature(inputCount, new Script(secondTransactionOpenOutput.hash), sigHashAll, true);
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
}
