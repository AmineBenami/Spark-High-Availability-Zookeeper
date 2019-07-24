package com.databootcamp.hbase;


import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.util.Bytes;
import com.databootcamp.hbase.CounterMap.Counter;


public class HBaseCounterIncrementor {

    static HBaseCounterIncrementor singleton;
    private String tableName;
    private String nameSpace;
    private String columnFamily;
    private String zookeeperHosts;
    private int zookeeperPort;
    private ConnectionTable hTable;
    private long lastUsed;
    private final long flushInterval = 1000;
    private CloserThread closerThread;
    private FlushThread flushThread;
    private HashMap<String, CounterMap> rowKeyCounterMap = new HashMap<String, CounterMap>();
    private static final Object locker = new Object();

    private HBaseCounterIncrementor(String _zookeeperHosts, int _zookeeperPort, String _nameSpace, String _tableName, String _columnFamily) {
        zookeeperHosts = _zookeeperHosts;
        zookeeperPort = _zookeeperPort;
        nameSpace = _nameSpace;
        tableName = _tableName;
        columnFamily = _columnFamily;
    }

    public static HBaseCounterIncrementor getInstance(String zookeeperHosts, int zookeeperPort, String nameSpace, String tableName, String columnFamily) {
        if (singleton == null) {
            synchronized (locker) {
                if (singleton == null) {
                    singleton = new HBaseCounterIncrementor(zookeeperHosts, zookeeperPort, nameSpace, tableName, columnFamily);
                    singleton.initialize();
                }
            }
        }
        return singleton;
    }

    private boolean initialize() {
        if (hTable == null || hTable.check() == false) {
            byte[] tablename = Bytes.toBytes(tableName);
            byte[] namespace = Bytes.toBytes(nameSpace);
            try {
                if (CreateTable.tableExists(zookeeperHosts, zookeeperPort, namespace, tablename) == true) {
                    hTable = CreateTable.createConnection(zookeeperHosts, zookeeperPort, namespace, tablename);
                    if (hTable != null && hTable.check()) {
                        updateLastUsed();
                    }
                } else {
                    System.err.println("---> initialize error: table " + tablename + " does not exists");
                }
            } catch (IOException e) {
                System.err.println("---> Error while initialization " + e);
            }
        }
        if (hTable == null || hTable.check() == false) {
            System.err.println("---> initialize Table fails");
            return false;
        }
        if (flushThread == null) {
            flushThread = new FlushThread(flushInterval);
        }
        if (flushThread != null && flushThread.isRunning() == false) {
            flushThread.start();
            try {
                Thread.sleep(400);
            } catch (Exception e) {
            }
        }
        if (flushThread == null || flushThread.isRunning() == false) {
            System.err.println("---> initialize cannot start flushThread");
            return false;
        }
        if (closerThread == null) {
            closerThread = new CloserThread();
        }
        if (closerThread != null && closerThread.isRunning() == false) {
            closerThread.start();
            try {
                Thread.sleep(400);
            } catch (Exception e) {

            }
        }
        if (closerThread == null || closerThread.isRunning() == false) {
            System.err.println("---> initialize cannot start closerThread");
            return false;
        }
        return true;
    }

    public void incerment(String rowKey, String key, int increment) {
        incerment(rowKey, key, (long) increment);
    }

    public void incerment(String rowKey, String key, long increment) {
        synchronized (locker) {
            CounterMap counterMap = rowKeyCounterMap.get(rowKey);
            if (counterMap == null) {
                counterMap = new CounterMap();
                rowKeyCounterMap.put(rowKey, counterMap);
            }
            counterMap.increment(key, increment);
            if (hTable == null || hTable.check() == false) {
                initialize();
            }
        }
    }

    private void updateLastUsed() {
        lastUsed = System.currentTimeMillis();
    }


    protected void close() {
        synchronized (locker) {
            if (hTable != null) {
                try {
                    hTable.close();
                } catch (IOException e) {
                    System.err.println("Error at closing connection tentative " + e.getMessage());
                }
                hTable = null;
            }
        }
    }

    protected static class CloserThread extends Thread {

        private boolean continueLoop = false;

        @Override
        public void run() {
            int notraffic = 0;
            continueLoop = true;
            while (continueLoop) {
                if (System.currentTimeMillis() - singleton.lastUsed > 30000) {
                    if (notraffic > 3 || singleton.rowKeyCounterMap.size() == 0) {
                        System.out.println("no traffic detected, shall close connection to database");
                        singleton.close();
                        notraffic = 0;
                    }
                    notraffic += 1;
                } else {
                    notraffic = 0;
                }

                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                }
            }
        }

        public boolean isRunning() {
            return continueLoop;
        }

        public void stopLoop() {
            continueLoop = false;
        }
    }

    protected static class FlushThread extends Thread {
        private long sleepTime;
        private boolean continueLoop = false;

        public FlushThread(long sleepTime) {
            this.sleepTime = sleepTime;
        }

        @Override
        public void run() {
            continueLoop = true;
            while (continueLoop) {
                try {
                    flushToHBase();
                } catch (IOException e) {
                    System.err.println("Exception ::: run flush to HBase : " + e.getMessage());
                    break;
                }

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                }
            }
            System.err.println("thread flush to database exit");
        }

        private void flushToHBase() throws IOException {
            synchronized (locker) {
                Set<String> commitedKeys = new HashSet<String>();
                for (Entry<String, CounterMap> entry : singleton.rowKeyCounterMap.entrySet()) {
                    Increment increment = new Increment(Bytes.toBytes(entry.getKey()));
                    boolean hasColumns = false;
                    for (Entry<String, Counter> entry2 : entry.getValue().entrySet()) {
                        increment.addColumn(Bytes.toBytes(singleton.columnFamily),
                                Bytes.toBytes(entry2.getKey()), entry2.getValue().value);
                        hasColumns = true;
                    }
                    if (hasColumns == true) {
                        if (singleton.initialize() == false) {
                            System.err.println("----> shall initialise before updating table but there is an error at this level, cannot increment key [" + entry.getKey() + "]");
                        } else {
                            try {
                                singleton.hTable.table.increment(increment);
                                singleton.updateLastUsed();
                                commitedKeys.add(entry.getKey());
                            } catch (IOException e) {
                                System.err.println("---->cannot push to hbase, cannot increment key [" + entry.getKey() + "] " + e.getMessage());
                            }
                        }
                    }
                }
                singleton.rowKeyCounterMap.keySet().removeAll(commitedKeys);
            }
        }

        public boolean isRunning() {
            return continueLoop;
        }

        public void stopLoop() {
            continueLoop = false;
        }
    }
}
