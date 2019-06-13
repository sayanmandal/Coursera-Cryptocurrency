package com.example.java;


import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

    private UTXOPool uPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.uPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

        Set<UTXO> utxoSet = new HashSet<UTXO>();
        // IMPLEMENT THIS
        ArrayList<Transaction.Input> ipList = tx.getInputs();
        ArrayList<Transaction.Output> opList = tx.getOutputs();

        /* Condition (1) */
        for(int i = 0 ; i < ipList.size() ; i++){
            Transaction.Input inp = ipList.get(i);
            if(inp == null) return false;
            UTXO utxo = new UTXO(inp.prevTxHash, inp.outputIndex);
            if(this.uPool.contains(utxo) == false)
                return false;
        }



        /* Condition (2) */
        for(int i = 0 ; i < ipList.size() ; i++){
            Transaction.Input inp = ipList.get(i);
            if(inp == null) return false;
            UTXO utxo = new UTXO(inp.prevTxHash, inp.outputIndex);
            Transaction.Output Coutput = uPool.getTxOutput(utxo);
            PublicKey pubKey = Coutput.address;
            if(Crypto.verifySignature(pubKey, tx.getRawDataToSign(i), inp.signature) == false)
                return false;

        }


        /* Condition (3) */
        for(int i = 0 ; i < ipList.size() ; i++){
            Transaction.Input inp = ipList.get(i);
            if(inp == null) return false;
            UTXO utxo = new UTXO(inp.prevTxHash, inp.outputIndex);
            if(utxoSet.contains(utxo) == true)
                return false;
            utxoSet.add(utxo);
        }


        /* Condition (4) */
        for(int i = 0 ; i < opList.size() ; i++){
            Transaction.Output op = opList.get(i);
            if(op == null)  return false;
            if(op.value < 0)
                return false;
        }

        /* Condition (5) */
        double inpVal = 0;
        double opVal = 0;
        for(int i = 0 ; i < ipList.size() ; i++){
            Transaction.Input inp = ipList.get(i);
            if(inp == null) return false;
            UTXO utxo = new UTXO(inp.prevTxHash, inp.outputIndex);
            Transaction.Output Coutput = uPool.getTxOutput(utxo);
            inpVal += Coutput.value;
        }

        for(int i = 0 ; i < opList.size() ; i++){
            Transaction.Output op = opList.get(i);
            if(op == null)  return false;
            opVal += op.value;
        }

        if(inpVal < opVal)
            return false;


        return true;
    }


    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        List<Transaction> validTransactions = new ArrayList<Transaction>();
        for(Transaction tx: possibleTxs){
            if (isValidTx(tx)) {
                validTransactions.add(tx);
                ArrayList<Transaction.Input> ipList = tx.getInputs();
                ArrayList<Transaction.Output> opList = tx.getOutputs();

                for(int i = 0 ; i < ipList.size() ; i++){
                    Transaction.Input inp = ipList.get(i);
                    UTXO utxo = new UTXO(inp.prevTxHash, inp.outputIndex);
                    uPool.removeUTXO(utxo);
                }

                byte[] txHash = tx.getHash();
                for(int i = 0 ; i < opList.size() ; i++){
                    Transaction.Output op = opList.get(i);
                    UTXO utxo = new UTXO(txHash, i);
                    uPool.addUTXO(utxo, op);
                }
            }
        }

        return validTransactions.toArray(new Transaction[validTransactions.size()]);
    }

}
