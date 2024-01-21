package org.jupnp.tool.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.jupnp.QueueingThreadPoolExecutor;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends the {@link QueueingThreadPoolExecutor} about monitoring of
 * tasks executed.
 */
public class MonitoredQueueingThreadPoolExecutor extends QueueingThreadPoolExecutor {

    static boolean DEBUG_STATISTICS = false;

    /** Statistical data collected. */
    private MonitoredQueueingThreadPoolExecutor.Statistics stats;

    static final Logger logger = LoggerFactory.getLogger(MonitoredQueueingThreadPoolExecutor.class);
    static final Logger statsLogger = LoggerFactory.getLogger("org.jupnp.tool.cli.stats");

    public MonitoredQueueingThreadPoolExecutor(String poolName, int threadPoolSize) {
        super(poolName, threadPoolSize);
        logger.debug("Created MonitoredQueueingThreadPoolExecutor with poolName={} and poolSize={}", poolName,
                threadPoolSize);
        if (DEBUG_STATISTICS) {
            stats = new Statistics(poolName);
        }
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        if (DEBUG_STATISTICS) {
            stats.addCurrentPoolSize(this);
            stats.addExcecutor(r);
        }
        // TODO why so much executors?
        // if (getQueue().size() > 100) {
        // System.out.println("ALERT");
        // }
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        if (throwable != null) {
            Throwable cause = Exceptions.unwrap(throwable);
            if ((cause instanceof InterruptedException) && isTerminating()) {
                // Ignore this, might happen when we shutdownNow() the
                // executor. We can't
                // log at this point as the logging system might be stopped
                // already (e.g. if it's a CDI component).
                return;
            }
            // Log only
            logger.warn("Thread terminated {} abruptly with exception", runnable, throwable);
        }
    }

    @Override
    public void shutdown() {
        logger.info("shutdown");
        super.shutdown();
        if (DEBUG_STATISTICS) {
            stats.dumpPoolStats();
            stats.dumpExecutorsStats();
            stats.release();
        }
        logger.info("shutdown done");
    }

    @Override
    public List<Runnable> shutdownNow() {
        logger.info("shutdownNow");
        List<Runnable> res = super.shutdownNow();
        if (DEBUG_STATISTICS) {
            stats.dumpPoolStats();
            stats.dumpExecutorsStats();
            stats.release();
        }
        logger.info("shutdownNow done");
        return res;
    }

    // inner classes for statistics

    static class Statistics {

        static class PoolStatPoint {
            public long timestamp, completedTasks;
            public int corePoolSize, poolSize, maxPoolSize, activeCounts, queueSize;
        }

        /** Thread safe collection for points. */
        private List<Statistics.PoolStatPoint> points = new CopyOnWriteArrayList<>();

        /** Thread safe collection for executors. */
        private ConcurrentHashMap<String, AtomicInteger> executors = new ConcurrentHashMap<>();

        private final String poolName;

        Statistics(String name) {
            poolName = name;
        }

        /**
         * Add info about current pool status.
         */
        private void addCurrentPoolSize(ThreadPoolExecutor pool) {
            Statistics.PoolStatPoint p = new PoolStatPoint();
            p.timestamp = System.currentTimeMillis();
            p.corePoolSize = pool.getCorePoolSize();
            p.poolSize = pool.getPoolSize();
            p.maxPoolSize = pool.getMaximumPoolSize();
            p.activeCounts = pool.getActiveCount();
            p.queueSize = pool.getQueue().size();
            p.completedTasks = pool.getCompletedTaskCount();
            points.add(p);
        }

        /**
         * Increase number of calls to this runnable (by class name).
         */
        public void addExcecutor(Runnable r) {
            executors.putIfAbsent(r.getClass().getName(), new AtomicInteger(0));
            executors.get(r.getClass().getName()).incrementAndGet();
        }

        public void release() {
            points = null;
            executors = null;
        }

        public void dumpPoolStats() {
            statsLogger.info("Dump Pool Statistics for poolName: {}", poolName);
            statsLogger.info("[timestamp,corePoolSize,poolSize,maxPoolSize,activeThreads,queueSize,completedTasks]");
            for (Iterator<Statistics.PoolStatPoint> iter = points.iterator(); iter.hasNext();) {
                Statistics.PoolStatPoint p = iter.next();
                statsLogger.info("{},{},{},{},{},{},{}", p.timestamp, p.corePoolSize, p.poolSize, p.maxPoolSize,
                        p.activeCounts, p.queueSize, p.completedTasks);
            }
            statsLogger.info(" ");
        }

        public void dumpExecutorsStats() {
            statsLogger.info("Dump Pool Executors for poolName: {}", poolName);

            List<ConcurrentHashMap.Entry<String, AtomicInteger>> entries = new ArrayList<>(executors.entrySet());
            // sort the entries by number of calls
            Collections.sort(entries, new Comparator<>() {
                public int compare(ConcurrentHashMap.Entry<String, AtomicInteger> a,
                        ConcurrentHashMap.Entry<String, AtomicInteger> b) {
                    return Integer.compare(b.getValue().get(), a.getValue().get());
                }
            });

            statsLogger.info("[executorClassName,numberOfExecutes]");
            for (ConcurrentHashMap.Entry<String, AtomicInteger> e : entries) {
                statsLogger.info("{},{}", e.getKey(), e.getValue().get());
            }
            statsLogger.info(" ");
        }
    }
}
