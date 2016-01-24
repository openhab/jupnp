/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.jupnp.test.service;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.security.Permission;
import java.util.Random;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.jupnp.QueueingThreadPoolExecutor;

/**
 * Test class for the {@link QueueingThreadPoolExecutor} class, abbreviated here
 * QueueingTPE.
 * 
 * @author Jochen Hiller - Initial contribution
 */
public class QueueingThreadPoolExecutorTest {

	/**
	 * we know that the QueuingTPE uses a core pool timeout of 10 seconds. Will
	 * be needed to check if all threads are down after this timeout.
	 */
	private final static int CORE_POOL_TIMEOUT = 10000;

	/**
	 * We can enable logging for all test cases.
	 */
	@BeforeTest
	public void setUp() {
		// enable to see logging. See below how to include slf4j-simple
		// enableLogging();
		disableLogging();
	}

	/**
	 * Creates QueueingTPE instances. By default there will be NO thread
	 * created, check it.
	 */
	@Test
	public void testCreateInstance() {
		String poolName = "testCreateInstance";
		QueueingThreadPoolExecutor.createInstance(poolName, 1);
		QueueingThreadPoolExecutor.createInstance(poolName, 2);
		QueueingThreadPoolExecutor.createInstance(poolName, 5);
		QueueingThreadPoolExecutor.createInstance(poolName, 10);
		QueueingThreadPoolExecutor.createInstance(poolName, 1000);
		QueueingThreadPoolExecutor.createInstance(poolName, 10000);
		QueueingThreadPoolExecutor.createInstance(poolName, 100000);
		QueueingThreadPoolExecutor.createInstance(poolName, 1000000);
		// no threads created
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	/**
	 * Tests what happens when poolName == null.
	 * 
	 * @TODO should QTPE raise an IllegalArgumentException in that case?
	 */
	@Test
	public void testCreateInstanceInvalidArgsPoolNameNull() throws InterruptedException {
		String poolName = "null";
		ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(null, 1);
		pool.execute(createRunnableFast());
		assertTrue(isPoolThreadActive(poolName, 1));
		pool.execute(createRunnableFast());
		pool.execute(createRunnableFast());
		pool.execute(createRunnableFast());
		assertTrue(isQueueThreadActive(poolName));

		// needs to wait CORE_POOL_TIMEOUT + 2sec-queue-thread + x until all
		// threads are down again
		pool.shutdown();
		Thread.sleep(CORE_POOL_TIMEOUT + 3000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testCreateInstanceInvalidArgsPoolSize0() {
		QueueingThreadPoolExecutor.createInstance("test", 0);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testCreateInstanceInvalidArgsPoolSizeMinus1() {
		QueueingThreadPoolExecutor.createInstance("test", -1);
	}

	/**
	 * This test tests behavior of standard TPE for a pool of core=1, max=2 when
	 * no tasks have been scheduled. Acts as reference test case.
	 */
	@Test
	public void testPlainTPEPoolSize2() throws InterruptedException {
		ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 2, 10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		pool.allowCoreThreadTimeOut(true);

		assertEquals(pool.getActiveCount(), 0);
		assertEquals(pool.allowsCoreThreadTimeOut(), true);
		assertEquals(pool.getCompletedTaskCount(), 0);
		assertEquals(pool.getCorePoolSize(), 1);
		assertEquals(pool.getMaximumPoolSize(), 2);
		assertEquals(pool.getLargestPoolSize(), 0);
		assertEquals(pool.getQueue().size(), 0);

		// there will be 1 core thread created. Will not check here

		// needs to wait CORE_POOL_TIMEOUT + x until all threads are down again
		pool.shutdown();
		Thread.sleep(CORE_POOL_TIMEOUT + 1000);
	}

	/**
	 * This test tests behavior of QueueingTPE for a pool of core=1, max=2 when
	 * no tasks have been scheduled. Same assumptions as above.
	 */
	@Test
	public void testQueuingTPEPoolSize2() throws InterruptedException {
		String poolName = "testQueuingTPEPoolSize2";
		ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);

		assertEquals(pool.getActiveCount(), 0);
		assertEquals(pool.allowsCoreThreadTimeOut(), true);
		assertEquals(pool.getCompletedTaskCount(), 0);
		assertEquals(pool.getCorePoolSize(), 1);
		assertEquals(pool.getMaximumPoolSize(), 2);
		assertEquals(pool.getLargestPoolSize(), 0);
		assertEquals(pool.getQueue().size(), 0);

		// now expect that no threads have been created
		assertFalse(areThreadsFromPoolRunning(poolName));

		// no need to wait after shutdown as no threads created
		pool.shutdown();
	}

	@Test
	public void testPoolWithWellDefinedPoolName() throws InterruptedException {
		basicTestForPoolName("testPoolWithWellDefinedPoolName");
	}

	@Test
	public void testPoolWithBlankPoolName() throws InterruptedException {
		basicTestForPoolName(" ");
	}

	@Test
	public void testPoolWithEmptyPoolName() throws InterruptedException {
		basicTestForPoolName("");
	}

	/**
	 * Basic tests of a pool with a given name. Checks thread creation and
	 * cleanup.
	 */
	protected void basicTestForPoolName(String poolName) throws InterruptedException {
		ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);
		pool.execute(createRunnable100ms());
		pool.execute(createRunnable100ms());
		assertTrue(isPoolThreadActive(poolName, 1));
		assertTrue(isPoolThreadActive(poolName, 2));

		// no queue thread

		// needs to wait CORE_POOL_TIMEOUT + x until all threads are down again
		pool.shutdown();
		Thread.sleep(CORE_POOL_TIMEOUT + 1000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	@Test
	public void testPoolSize1() throws InterruptedException {
		String poolName = "testPoolSize1";
		ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 1);

		pool.execute(createRunnable100ms());
		assertEquals(pool.getActiveCount(), 1);

		pool.execute(createRunnable100ms());
		// queue thread must be active
		assertTrue(isQueueThreadActive(poolName));
		// after 1+x sec all should be executed
		Thread.sleep(1000);
		assertEquals(pool.getCompletedTaskCount(), 2);
		// at the end queue thread is shutdown, wait for additional 3 sec
		Thread.sleep(3000);
		assertFalse(isQueueThreadActive(poolName));

		pool.execute(createRunnable100ms());
		pool.execute(createRunnable100ms());
		assertTrue(isQueueThreadActive(poolName));
		// after 1+x sec all should be executed
		Thread.sleep(1000);
		assertEquals(pool.getCompletedTaskCount(), 4);

		// needs to wait CORE_POOL_TIMEOUT + 2sec-queue-thread + x until all
		// threads are down again
		pool.shutdown();
		Thread.sleep(CORE_POOL_TIMEOUT + 3000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	@Test
	public void testPoolSize2ThreadsFast() throws InterruptedException {
		String poolName = "testPoolSize2ThreadsFast";
		ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);
		pool.execute(createRunnableFast());
		pool.execute(createRunnableFast());
		Thread.sleep(1000);
		assertEquals(pool.getCompletedTaskCount(), 2);

		// no queue thread

		// needs to wait CORE_POOL_TIMEOUT + x until all threads are down again
		pool.shutdown();
		Thread.sleep(CORE_POOL_TIMEOUT + 1000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	@Test
	public void testPoolSize1000ThreadsFast() throws InterruptedException {
		AbstractRunnable.resetRuns();
		String poolName = "testPoolSize1000ThreadsFast";
		ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 1000);
		for (int i = 0; i < 1000; i++) {
			pool.execute(createRunnableFast());
		}
		// no queue thread
		assertFalse(isQueueThreadActive(poolName));
		Thread.sleep(1000);
		assertEquals(pool.getCompletedTaskCount(), 1000);
		assertEquals(AbstractRunnable.getRuns(), 1000);

		// needs to wait CORE_POOL_TIMEOUT + x until all threads are down again
		pool.shutdown();
		Thread.sleep(CORE_POOL_TIMEOUT + 1000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	@Test
	public void testPoolSize2ThreadsHeavyLoad() throws InterruptedException {
		String poolName = "testPoolSize2ThreadsHeavyLoad";
		ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);
		pool.execute(createRunnableHeavyLoad1s());
		pool.execute(createRunnableHeavyLoad1s());
		pool.execute(createRunnableHeavyLoad1s());
		pool.execute(createRunnableHeavyLoad1s());

		assertTrue(isQueueThreadActive(poolName));
		Thread.sleep(5000);
		assertEquals(pool.getCompletedTaskCount(), 4);

		// needs to wait CORE_POOL_TIMEOUT + 2sec-queue-thread + x until all
		// threads are down again
		pool.shutdown();
		Thread.sleep(CORE_POOL_TIMEOUT + 3000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	@Test
	public void testPoolSize2ThreadSettings() throws InterruptedException {
		String poolName = "testPoolSize2ThreadSettings";
		basicTestPoolSize2ThreadSettings(poolName);
	}

	/**
	 * Tests using security manager with high max thread group priority.
	 */
	@Test
	public void testPoolSize2ThreadSettingsWithSeurityManagerThreadGroupMaxPriority() throws InterruptedException {
		SecurityManager sm = System.getSecurityManager();
		try {
			System.setSecurityManager(new SecurityManager() {
				@Override
				public void checkPermission(Permission perm) {
				}

				@Override
				public ThreadGroup getThreadGroup() {
					ThreadGroup g = new ThreadGroup("TestMaxThreadGroup");
					g.setDaemon(true);
					g.setMaxPriority(Thread.MAX_PRIORITY);
					return g;
				}
			});
			String poolName = "testPoolSize2ThreadSettingsWithSeurityManagerThreadGroupMaxPriority";
			basicTestPoolSize2ThreadSettings(poolName);
		} finally {
			System.setSecurityManager(sm);
		}
	}

	/**
	 * Tests using security manager with low min thread group priority.
	 */
	@Test
	public void testPoolSize2ThreadSettingsWithSeurityManagerThreadGroupMinPriority() throws InterruptedException {
		SecurityManager sm = System.getSecurityManager();
		try {
			System.setSecurityManager(new SecurityManager() {
				@Override
				public void checkPermission(Permission perm) {
				}

				@Override
				public ThreadGroup getThreadGroup() {
					ThreadGroup g = new ThreadGroup("TestMinThreadGroup");
					g.setDaemon(true);
					g.setMaxPriority(Thread.MIN_PRIORITY);
					return g;
				}
			});
			String poolName = "testPoolSize2ThreadSettingsWithSeurityManagerThreadGroupMinPriority";
			basicTestPoolSize2ThreadSettings(poolName);
		} finally {
			System.setSecurityManager(sm);
		}
	}

	/**
	 * Test basic thread creation, including thread settings (name, prio,
	 * daemon).
	 */
	protected void basicTestPoolSize2ThreadSettings(String poolName) throws InterruptedException {
		ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);

		// pool 2 tasks, threads must exist
		pool.execute(createRunnable10s());
		assertEquals(pool.getActiveCount(), 1);
		assertTrue(isPoolThreadActive(poolName, 1));
		Thread t1 = getThread(poolName + "-1");
		assertEquals(t1.isDaemon(), false);
		// thread will be NORM prio or max prio of this thread group, which can
		// < than NORM
		int prio1 = Math.min(t1.getThreadGroup().getMaxPriority(), Thread.NORM_PRIORITY);
		assertEquals(t1.getPriority(), prio1);

		pool.execute(createRunnable10s());
		assertEquals(pool.getActiveCount(), 2);
		assertTrue(isPoolThreadActive(poolName, 2));
		Thread t2 = getThread(poolName + "-2");
		assertEquals(t2.isDaemon(), false);
		// thread will be NORM prio or max prio of this thread group, which can
		// < than NORM
		int prio2 = Math.min(t2.getThreadGroup().getMaxPriority(), Thread.NORM_PRIORITY);
		assertEquals(t2.getPriority(), prio2);

		// 2 more tasks, will be queued, no threads
		pool.execute(createRunnable1s());
		// as pool size is 2, no more active threads, will stay at 2
		assertEquals(pool.getActiveCount(), 2);
		assertFalse(isPoolThreadActive(poolName, 3));
		assertEquals(pool.getQueue().size(), 1);

		pool.execute(createRunnable1s());
		assertEquals(pool.getActiveCount(), 2);
		assertFalse(isPoolThreadActive(poolName, 4));
		assertEquals(pool.getQueue().size(), 2);

		// 0 are yet executed
		assertEquals(pool.getCompletedTaskCount(), 0);

		// needs to wait CORE_POOL_TIMEOUT + 2sec-queue-thread + x until all
		// threads are down again
		pool.shutdown();
		Thread.sleep(CORE_POOL_TIMEOUT + 3000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	/**
	 * Test that no further tasks will be executed when TPE is terminating.
	 * 
	 * @TODO the check for
	 *       "(threadPoolExecutor instanceof QueueingThreadPoolExecutor)" is not
	 *       necessary. This can never happen
	 */
	@Test
	public void testShutdownNoEntriesIntoQueueAnymore() throws InterruptedException {
		String poolName = "testShutdownNoEntriesIntoQueueAnymore";
		ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);

		pool.execute(createRunnable10s());
		pool.execute(createRunnable10s());
		pool.execute(createRunnable10s());
		assertEquals(pool.getQueue().size(), 1);
		pool.execute(createRunnable10s());
		assertEquals(pool.getQueue().size(), 2);

		// now shutdown, and check no more entries into pool
		pool.shutdown();
		// give chance to shutdown
		Thread.sleep(1000);
		assertEquals(pool.isShutdown(), true);
		pool.execute(createRunnable10s());
		// must stay at 2
		assertEquals(pool.getQueue().size(), 2);

		// 0 are executed
		assertEquals(pool.getCompletedTaskCount(), 0);

		// needs to wait CORE_POOL_TIMEOUT + 2sec-queue-thread + x until all
		// threads are down again
		// pool yet shutdown here
		Thread.sleep(CORE_POOL_TIMEOUT + 3000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	/**
	 * Tests what happens when wrong rejected execution handler will be used.
	 * 
	 * @TODO better to override setRejectedExecutionHandler to NOT allow wrong
	 *       one?
	 */
	@Test
	public void testSetInvalidRejectionHandler() throws InterruptedException {
		String poolName = "testShutdownNoEntriesIntoQueueAnymore";
		ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);

		pool.execute(createRunnable100ms());
		pool.execute(createRunnable100ms());
		pool.execute(createRunnable100ms());
		pool.execute(createRunnable100ms());
		assertEquals(pool.getActiveCount(), 2);
		assertEquals(pool.getQueue().size(), 2);

		// with wrong execution handler, further entries will be ignored
		pool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
		pool.execute(createRunnableFast());
		pool.execute(createRunnableFast());
		assertEquals(pool.getQueue().size(), 2);

		// needs to wait CORE_POOL_TIMEOUT + 2sec-queue-thread + x until all
		// threads are down again
		// pool yet shutdown here
		Thread.sleep(CORE_POOL_TIMEOUT + 3000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	/**
	 * Test that interrupting queue thread does not loose tasks.
	 */
	@Test
	public void testQueueThreadInterrupt() throws InterruptedException {
		String poolName = "testQueueThreadInterrupt";
		final ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);
		long tStart;
		long tEnd;

		// interrupt when queue thread is started from current thread
		for (int i = 0; i < 100; i++) {
			pool.execute(createRunnable100ms());
			Thread queueThread = getThread(poolName + "-queue");
			if (queueThread != null) {
				queueThread.interrupt();
			}
		}
		assertEquals(pool.getQueue().size(), 98);
		tStart = System.currentTimeMillis();
		tEnd = tStart;
		// wait for 10 sec
		while ((tEnd - tStart) < 10000) {
			// send interrupt very fast to jump into every piece of code
			for (int j = 0; j < 100; j++) {
				Thread queueThread = getThread(poolName + "-queue");
				if (queueThread != null) {
					queueThread.interrupt();
				}
			}
			Thread.sleep(50); // chance for thread switch
			tEnd = System.currentTimeMillis();
		}
		// chance to finalize queue thread
		Thread.sleep(3000);
		assertFalse(isQueueThreadActive(poolName));
		// all should be executed
		assertEquals(pool.getCompletedTaskCount(), 100);

		// interrupt when queue thread is started from other threads
		for (int i = 0; i < 100; i++) {
			new Thread() {
				public void run() {
					pool.execute(createRunnable100ms());
				}
			}.start();
			Thread queueThread = getThread(poolName + "-queue");
			if (queueThread != null) {
				queueThread.interrupt();
			}
		}
		tStart = System.currentTimeMillis();
		tEnd = tStart;
		// wait for 10 sec
		while ((tEnd - tStart) < 10000) {
			// send interrupt very fast to jump into every piece of code
			for (int j = 0; j < 100; j++) {
				Thread queueThread = getThread(poolName + "-queue");
				if (queueThread != null) {
					queueThread.interrupt();
				}
			}
			Thread.sleep(50); // chance for thread switch
			tEnd = System.currentTimeMillis();
		}
		// chance to finalize
		Thread.sleep(1000);
		// all should be executed
		assertEquals(pool.getCompletedTaskCount(), 200);

		// needs to wait CORE_POOL_TIMEOUT + 2sec-queue-thread + x until all
		// threads are down again
		pool.shutdown();
		Thread.sleep(CORE_POOL_TIMEOUT + 3000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	/**
	 * Tests large pool with long running tasks.
	 */
	@Test
	public void testPoolSize10ThreadsLongNewQueueThread() throws InterruptedException {
		String poolName = "testPoolSize10ThreadsLong";
		ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 10);

		for (int i = 0; i < 100; i++) {
			pool.execute(createRunnable100ms());
		}
		assertEquals(pool.getActiveCount(), 10);
		// queue thread must be active
		assertTrue(isQueueThreadActive(poolName));
		long queueThreadId1 = getThread(poolName + "-queue").getId();
		// after 10+x sec all should be executed
		Thread.sleep(12000);
		assertEquals(pool.getCompletedTaskCount(), 100);
		// at the end queue thread is shutdown, wait for additional 3 sec
		Thread.sleep(3000);
		assertFalse(isQueueThreadActive(poolName));

		// now put again requests into queue. Queue thread should be created
		// again with different thread id
		for (int i = 0; i < 100; i++) {
			pool.execute(createRunnable100ms());
		}
		assertTrue(isQueueThreadActive(poolName));
		long queueThreadId2 = getThread(poolName + "-queue").getId();
		// as queue thread has been created again, has to have different thread
		// id
		assertNotEquals(queueThreadId1, queueThreadId2);

		// after 10+x sec all should be executed
		Thread.sleep(12000);
		assertEquals(pool.getCompletedTaskCount(), 200);
		// at the end queue thread is shutdown, wait for additional 3 sec
		Thread.sleep(3000);
		assertFalse(isQueueThreadActive(poolName));

		// needs to wait CORE_POOL_TIMEOUT + x until all
		// threads are down again
		pool.shutdown();
		Thread.sleep(CORE_POOL_TIMEOUT + 1000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	/**
	 * This tests checks addToQueue() when used from multiple threads.
	 */
	@Test
	public void testPoolSize10FillPoolParallel() throws InterruptedException {
		String poolName = "testPoolSize10FillPoolParallel";
		final ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 10);

		Thread[] fillThreads = new Thread[100];
		for (int i = 0; i < fillThreads.length; i++) {
			fillThreads[i] = new Thread() {
				@Override
				public void run() {
					pool.execute(createRunnable100ms());
				}
			};
		}
		// now start all threads
		for (int i = 0; i < fillThreads.length; i++) {
			fillThreads[i].start();
		}
		Thread.sleep(1000); // wait until filled

		assertEquals(pool.getActiveCount(), 10);
		// queue thread must be active
		assertTrue(isQueueThreadActive(poolName));
		// after 10+x sec all should be executed
		Thread.sleep(12000);
		assertEquals(pool.getCompletedTaskCount(), 100);
		// at the end queue thread is shutdown, wait for additional 3 sec
		Thread.sleep(3000);
		assertFalse(isQueueThreadActive(poolName));

		// needs to wait CORE_POOL_TIMEOUT + x until all
		// threads are down again
		pool.shutdown();
		Thread.sleep(CORE_POOL_TIMEOUT + 1000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	/**
	 * Monkey test, just adding executors, and check if all have been executed.
	 */
	@Test
	public void testMonkeyTest() throws InterruptedException {
		AbstractRunnable.resetRuns();
		String poolName = "testMonkeyTest";
		final ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 5);

		Thread[] fillThreads = new Thread[100];
		for (int i = 0; i < fillThreads.length; i++) {
			fillThreads[i] = new Thread() {
				@Override
				public void run() {
					int action = new Random().nextInt(5);
					switch (action) {
					case 0:
						pool.execute(createRunnableFast());
						break;
					case 1:
						pool.execute(createRunnable100ms());
						break;
					case 2:
						pool.execute(createRunnable1s());
						break;
					case 3:
						pool.execute(createRunnable10s());
						break;
					case 4:
						pool.execute(createRunnableHeavyLoad1s());
						break;
					}
				}
			};
		}
		// now start all threads
		for (int i = 0; i < fillThreads.length; i++) {
			fillThreads[i].start();
		}
		Thread.sleep(1000); // wait until filled

		assertEquals(pool.getActiveCount(), 5);
		// queue thread must be active
		assertTrue(isQueueThreadActive(poolName));

		// wait until processed
		while (pool.getCompletedTaskCount() < 100) {
			Thread.sleep(1000);
		}
		assertEquals(pool.getCompletedTaskCount(), 100);
		// check runs too
		assertEquals(AbstractRunnable.getRuns(), 100);

		// needs to wait CORE_POOL_TIMEOUT + 2sec-queue-thread + x until all
		// threads are down again
		pool.shutdown();
		Thread.sleep(CORE_POOL_TIMEOUT + 3000);
		assertFalse(areThreadsFromPoolRunning(poolName));
	}

	// helper methods

	private void enableLogging() {
		// add slf4j-simple-1.7.13.jar to classpath to activate logging during
		// development
		// e.g. from http://mvnrepository.com/artifact/org.slf4j/slf4j-simple
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
		System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
		System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
	}

	private void disableLogging() {
		// disable logging
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
		System.clearProperty("org.slf4j.simpleLogger.logFile");
		System.clearProperty("org.slf4j.simpleLogger.showDateTime");
	}

	private boolean isQueueThreadActive(String poolName) {
		return getThread(poolName + "-queue") != null;
	}

	private boolean isPoolThreadActive(String poolName, int id) {
		return getThread(poolName + "-" + id) != null;
	}

	/**
	 * Search for thread with given name.
	 * 
	 * @return found thread or null
	 */
	private Thread getThread(String threadName) {
		// get top level thread group
		ThreadGroup g = Thread.currentThread().getThreadGroup();
		while (g.getParent() != null) {
			g = g.getParent();
		}
		// make buffer 10 entries bigger
		Thread[] l = new Thread[g.activeCount() + 10];
		int n = g.enumerate(l);
		for (int i = 0; i < n; i++) {
			// enable printout to see threads
			// System.out.println("getThread:" + l[i]);
			if (l[i].getName().equals(threadName)) {
				return l[i];
			}
		}
		return null;
	}

	private boolean areThreadsFromPoolRunning(String poolName) {
		// get top level thread group
		ThreadGroup g = Thread.currentThread().getThreadGroup();
		while (g.getParent() != null) {
			g = g.getParent();
		}
		boolean foundThreads = false;
		// make buffer 10 entries bigger
		Thread[] l = new Thread[g.activeCount() + 10];
		int n = g.enumerate(l);
		for (int i = 0; i < n; i++) {
			// we can only test if name is at least one character,
			// otherwise there will be threads found (handles poolName="")
			if (poolName.length() > 0) {
				if (l[i].getName().startsWith(poolName)) {
					// enable printout to see threads
					// System.out.println("areThreadsFromPoolRunning: " +
					// l[i].toString());
					foundThreads = true;
				}
			}
		}
		return foundThreads;
	}

	// Runnables for testing

	private Runnable createRunnableFast() {
		return new RunnableFast();
	}

	private Runnable createRunnable100ms() {
		return new Runnable100ms();
	}

	private Runnable createRunnable1s() {
		return new Runnable1s();
	}

	private Runnable createRunnable10s() {
		return new Runnable10s();
	}

	private Runnable createRunnableHeavyLoad1s() {
		return new RunnableHeavyLoad1s();
	}

	private static abstract class AbstractRunnable implements Runnable {
		private static AtomicInteger runs = new AtomicInteger(0);

		public static void resetRuns() {
			runs = new AtomicInteger(0);
		}

		public static int getRuns() {
			return runs.get();
		}

		protected Logger logger = LoggerFactory.getLogger(this.getClass());

		protected void sleep(int milliseconds) {
			try {
				Thread.sleep(milliseconds);
			} catch (InterruptedException e) {
				// ignore
				logger.info("interrupted");
			}
		}

		public void run() {
			logger.info("run");
			runs.incrementAndGet();
		}
	}

	private static class RunnableFast extends AbstractRunnable {
		@Override
		public void run() {
			super.run();
			// do nothing
		}
	}

	private static class RunnableHeavyLoad1s extends AbstractRunnable {
		/** preserve as static to avoid code optimization. */
		static double d = Math.PI;

		@Override
		public void run() {
			super.run();
			long tStart = System.currentTimeMillis();
			long tEnd = tStart;
			while ((tEnd - tStart) < 1000) {
				for (int i = 0; i < 10000; i++) {
					d += Math.acos((double) tEnd);
					d += Math.atan(Math.sqrt(Math.pow(d, 10)));
				}
				tEnd = System.currentTimeMillis();
			}
		}
	}

	private static class Runnable100ms extends AbstractRunnable {
		@Override
		public void run() {
			super.run();
			sleep(100);
		}
	}

	private static class Runnable1s extends AbstractRunnable {
		@Override
		public void run() {
			super.run();
			sleep(1 * 1000);
		}
	}

	private static class Runnable10s extends AbstractRunnable {
		@Override
		public void run() {
			super.run();
			sleep(10 * 1000);
		}
	}
}
