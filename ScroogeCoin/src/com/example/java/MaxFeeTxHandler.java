package com.example.java;


import java.security.PublicKey;
import java.util.*;

public class MaxFeeTxHandler{

    private UTXOPool uPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
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


    public class TransactionFee implements Comparable<TransactionFee>{
        private Transaction tx;
        private double fee;

        public TransactionFee(Transaction tx){
            this.tx = tx;
            this.fee = getFee(tx);
        }

        public Transaction getTransaction(){
            return this.tx;
        }

        public double GETFEE(){
            return this.fee;
        }

        @Override
        public int compareTo(TransactionFee ty){
            double rem = fee - ty.GETFEE();
            if(rem > 0) return 1;
            else if(rem < 0)    return -1;
            else    return 0;
        }
    }


    public double getFee(Transaction tx){
        List<Transaction.Input> inpList = tx.getInputs();
        List<Transaction.Output> opList = tx.getOutputs();

        double inpVal = 0;
        double opVal = 0;

        if(!isValidTx(tx))  return 0;


        for (int i = 0; i < inpList.size(); i++) {
            Transaction.Input inp = inpList.get(i);
            UTXO utxo = new UTXO(inp.prevTxHash, inp.outputIndex);
            Transaction.Output op = this.uPool.getTxOutput(utxo);
            inpVal += op.value;

        }


        for (int i = 0; i < opList.size(); i++) {
            Transaction.Output op = opList.get(i);
            opVal += op.value;

        }

        return inpVal - opVal;
    }


    public Transaction[] validTxs(Transaction[] possibleTxs){
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


    public void removeUTXO(Transaction tx){
        ArrayList<Transaction.Input> ipList = tx.getInputs();
        for(int i = 0 ; i < ipList.size() ; i++){
            Transaction.Input inp = ipList.get(i);
            UTXO utxo = new UTXO(inp.prevTxHash, inp.outputIndex);
            uPool.removeUTXO(utxo);
        }
    }


    public void addUTXO(Transaction tx){
        ArrayList<Transaction.Output> opList = tx.getOutputs();
        byte[] txHash = tx.getHash();
        for(int i = 0 ; i < opList.size() ; i++){
            Transaction.Output op = opList.get(i);
            UTXO utxo = new UTXO(txHash, i);
            uPool.addUTXO(utxo, op);
        }
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     * maximize total transaction fees
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        List<Transaction> validTransactions = new ArrayList<Transaction>();
        for(Transaction tx: possibleTxs){
            if(isValidTx(tx))
                validTransactions.add(tx);
        }

        int sz = validTransactions.size();

        int mask = (1<<sz);

        int greatmask = 0;
        double maxfee = 0;

        List<Transaction> dupTransactions = new ArrayList<Transaction>(validTransactions);
        UTXOPool dupPool = new UTXOPool(this.uPool);

        for(int i = 0 ; i < mask ; i++){
            double fee = 0;
            this.uPool = new UTXOPool(dupPool);
            for(int j = 0 ; j < sz ; j++){
                Transaction tx = validTransactions.get(j);
                if(((1<<j)&i) != 0){
                    if(isValidTx(tx)){
                        fee += getFee(tx);
                        removeUTXO(tx);
                        addUTXO(tx);
                    }
                }
            }

            if(fee > maxfee){
                maxfee = fee;
                greatmask = i;
            }
        }

        List<Transaction> result = new ArrayList<>();
        for(int i = 0 ; i < sz ; i++){
            if(((1<<i)&greatmask) != 0){
                result.add(validTransactions.get(i));
            }
        }

        return result.toArray(new Transaction[result.size()]);


    }

}
