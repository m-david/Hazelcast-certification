package com.hazelcast.certification.process;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.certification.domain.Transaction;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * This class starts Fraud Detection process.
 * This must extend <code>AbstractFraudDetectionServer</code> that initializes socket
 * connections, queues etc and starts the process of fraud detection by invoking
 * <code>startFraudDetection()</code>. <br>
 * Each Server instance listens to incoming transactions on a specific port i.e.
 * each Server instance has a dedicated port to receive transactions. <br>
 * All incoming transactions are read on a socket and put in a <b>Blocking Queue
 * </b>. Application can only obtain a credit card transaction from this
 * blocking queue using <code>getNextTxn()</code>. <br>
 * This app also allows to warm-up the NearCache client by setting a property
 * <code>doWarmup</code> to <code>true</code> in <code>FraudDetection.properties</code>.
 * In such case, <code>warmup()</code> will be invoked before starting the
 * process of fraud detection. <br>
 * To start the process of fraud detection for a given transaction, invoke
 * <code>processFraudDetection(...)</code> in <b>FraudDetectionProcess</b> within
 * <code>startFraudDetection()</code> <br>
 * 
 * For TPS monitoring, use <code>overallTPS</code> i.e. an AtomicInteger, to increment
 * at appropriate places, where required. The application continuously prints the 
 * TPS for the transactions done in previous 5 seconds, and resets the counter to 0.
 *    
 * @author rahul
 *
 */
public abstract class FraudDetection {

	private final static ILogger log = Logger.getLogger(FraudDetection.class);

	private AtomicInteger overallTPS;
	protected BlockingQueue<String> txnQueue;
	
	public void run() {
		startPerformanceMonitor();
		startFraudDetection();
	}
	
	public void bindTransactionQueue(BlockingQueue<String> queue) {
		this.txnQueue = queue;
	}
	
	protected AtomicInteger getTPSCounter() {
		return overallTPS;
	}

	private void startPerformanceMonitor() {
		overallTPS = new AtomicInteger();
		startTPSMonitor();
	}

	private void startTPSMonitor() {
		Thread monitor = new Thread() {
			public void run() {
				try {
					while (!Thread.interrupted()) {
						sleep(5000);
						log.info("Transactions processed per second = "
								+ (overallTPS.getAndSet(0) / 5));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		monitor.setDaemon(true);
		monitor.start();
	}

	protected Transaction getNextTxn() throws InterruptedException {
		return prepareTransaction(txnQueue.take());
	}

	protected Transaction prepareTransaction(String txnString) throws RuntimeException {
		Transaction txn = new Transaction();
		String[] cName = txnString.split(",");
		txn.setCreditCardNumber(cName[0]);
		txn.setTimeStamp(Long.parseLong(cName[1]));
		txn.setCountryCode(cName[2]);
		txn.setResponseCode(cName[3]);
		txn.setTxnAmt(cName[4]);
		txn.setMerchantType(cName[6]);
		txn.setTxnCity(cName[7]);
		txn.setTxnCode(cName[8]);
		return txn;
	}

	protected abstract void startFraudDetection();

}