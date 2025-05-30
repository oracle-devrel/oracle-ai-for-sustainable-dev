package oracleai.financial;

import com.opencsv.CSVWriter;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleShardingKey;
import oracle.jdbc.OracleType;
import oracle.jdbc.replay.OracleDataSourceImpl;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.common.UniversalConnectionPoolImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.Well19937c;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;


public class Transactions_Maa extends Thread {

    private static double version = 1.9;
    protected static PoolDataSource pds = null;
    protected static int NUM_OF_THREADS = 3;

    private static int POOL_SIZE = 0;

    private static int NUM_OF_TRANSACTION = 0;

    private static int MAX_POOL_SIZE = 3;

    protected static Map<Integer, String> acctIDShardMap = new HashMap<Integer, String>();

    protected static AtomicLong counter = new AtomicLong();

    protected static AtomicInteger errorcount = new AtomicInteger();

    public static List<Double> responseTime = new ArrayList<Double>();
    private static long idx = 0;
    private static String userName = "";
    private static String password = "";
    private static String url = "";
    private String countryId = "";

    private static String methodType = "none";

    protected static ConcurrentMap<String, AtomicLong> shardMap = new ConcurrentHashMap<>();

    protected static int shardSize = 3;
    private static String runStamp = "";

    private static String printDSN = "";

    private static String currentHostName = "";

    protected static String serviceName = "";

    public static CSVWriter csvWriter = null;

    public static CSVWriter paramWriter = null;

    private int accountId = -1;
    static Random rand = new Random();
    boolean bInfo = false;

    private int thr_id = -1;

    private static long pooltime = 0;

    private static String java_version = System.getProperty("java.version");

    private static String driver_version = "";

    private static String tnsAdminFile = "";
    //When set to true, the JDBC driver handles the sharding key and super sharding key for you, so you don't need to provide them explicitly in your application code.
    private static String transparent_ds = "N";

    private static int accountSamples = 1000000;
    private static HashMap<Integer, String> accountdetails = new HashMap<Integer, String>();

    private static AtomicInteger acctcounter = new AtomicInteger();
    private static ConcurrentMap<String, AtomicInteger> resultsSummary = new ConcurrentHashMap<>();
    private static List<Integer> acctList = new ArrayList<Integer>();

    private static AtomicBoolean bdisplayError = new AtomicBoolean();

    private static String verbose = "";

    protected static Timer timer = new Timer();

    private static int dur;

    private static Boolean bError = false;

    private static Boolean bSampleAcquistionComplete = true;

    private static final AtomicBoolean bStartTimerSet = new AtomicBoolean();

    //private static Instant runStartTime = Instant.now();

    private static int connection_Timeout = 120;// Setting the connection time to 2 minutes
    //private static Instant runStartTime =  Instant.now().plus(5, ChronoUnit.MINUTES);

    private static Instant runStartTime = Instant.now();

    private static int NO_OF_COLUMNS = 15;

    private static HashSet<String> warningMessageSet = new HashSet<String>();

    private static Instant connectionStarttime;

    private static long connectiontimeout_duration = 0;

    private static long testRun_duration = 0;

    private static int thinkTime = 0;

    private static String database_version = "";


    private static int sampleSize = 1;

    private static final int iterMax = 20;
    private static final int retrydelay = 3;

    private static final String[] countryArray =
            {"AW", "CA", "CR", "HT", "MX", "US", "AR", "BO", "BR", "CL", "CO", "VE"};

    private AtomicInteger currentSecond = new AtomicInteger(Calendar.getInstance().get(Calendar.SECOND));

    private AtomicLong count = new AtomicLong();

    public final String INFO = "SELECT   LOWER(d.db_unique_name || '_' || p.name) AS cdb_pdb\n" +
            "        ,i.instance_name\n" +
            "        ,i.status\n" +
            "        ,i.active_state\n" +
            "        ,i.database_type\n" +
            "FROM     v$pdbs      p,\n" +
            "         v$database  d,\n" +
            "         v$instance  i\n" +
            "ORDER BY i.instance_name";


    public final String SELECT = "SELECT user_id\n" +
            "   ,SYS_CONTEXT('USERENV', 'INSTANCE_NAME')\n" +
            "   ,(SELECT d.db_unique_name || '_' || p.name FROM v$pdbs p, v$database d)\n" +
            "   ,SYS_CONTEXT('USERENV', 'SID')\n" +
            "FROM   accounts\n" +
            "WHERE  account_id = ?\n" +
            "AND    country_cd = ? FOR UPDATE OF account_id";


    public final String INSERT = "INSERT INTO payments\n" +
            "(\n" +
            "id\n" +
            ",country_cd\n" +
            ",account_id\n" +
            ",amount\n" +
            ",created_utc\n" +
            ")\n" +
            "VALUES\n" +
            "(\n" +
            "payment_id_seq.NEXTVAL\n" +
            ",?\n" +
            ",?\n" +
            ",?\n" +
            ",SYS_EXTRACT_UTC(SYSTIMESTAMP)\n" +
            ")";

    public final String INSERTACCOUNTS = "INSERT INTO accounts\n" +
            "(\n" +
            "country_cd\n" +
            ",account_id\n" +
            ",user_id\n" +
            ",balance\n" +
            ",last_modified_utc\n" +
            ")\n" +
            "VALUES\n" +
            "(\n" +
            "?\n" +
            ",?\n" +
            ",?\n" +
            ",?\n" +
            ",SYS_EXTRACT_UTC(SYSTIMESTAMP)\n" +
            ")";

    public final String UPDATE = "UPDATE accounts\n" +
            "SET    balance   = balance - ?\n" +
            ",last_modified_utc = SYSTIMESTAMP AT TIME ZONE 'UTC'\n" +
            "WHERE  account_id    = ?\n" +
            "AND    country_cd       = ?";

    public static final String GETSAMPLE = "SELECT o.account_id, o.country_cd, c.rw_dbnum FROM ( SELECT i.account_id, i.country_cd, ora_hash(i.account_id) AS account_id_hash FROM accounts i ORDER BY dbms_random.value() FETCH FIRST 1000 ROWS ONLY )                        o, gsmadmin_internal.chunks c WHERE o.account_id_hash BETWEEN c.low_key AND c.high_key";

    public static final String GETCOUNT = "select count(*) from accounts";

    public static final String GETMAXACCOUNTID = "select max(account_id) from accounts";

    public Transactions_Maa(int tid) throws SQLException {
        //int randomElement = ThreadLocalRandom.current().nextInt(acctList.size()) % acctList.size();
        if(bSampleAcquistionComplete) {
            int randomElement;


            randomElement = acctList.get(rand.nextInt(acctList.size()));

            sampleSize = acctList.size();

            countryId = accountdetails.get(randomElement).toUpperCase();
            accountId = randomElement;
            thr_id = tid;
            //System.out.println("selected account id:"+ accountId);
        }
    }

    public static void create_pool() throws SQLException {
        //System.out.println("  -> Create UCP POOL " );
        pds = PoolDataSourceFactory.getPoolDataSource();
        // PoolDataSource and UCP configuration
        //set the connection properties on the data source and pool properties
        pds.setUser(userName);
        pds.setPassword(password);
        pds.setURL(url);
        pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        pds.setInitialPoolSize(MAX_POOL_SIZE);
        pds.setMinPoolSize(MAX_POOL_SIZE);
        pds.setFastConnectionFailoverEnabled(true);
        pds.setValidateConnectionOnBorrow(true);
        pds.setMaxPoolSize(MAX_POOL_SIZE);
        Properties connProps = new Properties();
        pds.setConnectionProperty(OracleConnection.CONNECTION_PROPERTY_AUTOCOMMIT, "false");
        if (tnsAdminFile != null && !tnsAdminFile.isEmpty()) {
            pds.setConnectionProperty(OracleConnection.CONNECTION_PROPERTY_TNS_ADMIN, tnsAdminFile);
        }
        if (transparent_ds.equalsIgnoreCase("Y")) {
            pds.setConnectionProperty("oracle.jdbc.useShardingDriverConnection", "true");
            pds.setConnectionProperty("oracle.jdbc.allowSingleShardTransactionSupport", "true");
        }

    }

    public static int randomInteger(int s, int e) {
        if ((e - s) != 0) {
            return (Math.abs(rand.nextInt()) % (e - s)) + s;
        }
        return e;
    }

    public void displayPoolDetails() throws SQLException, UniversalConnectionPoolException {
        System.out.println("-----------");
        System.out.println("NumberOfAvailableConnections: " + pds.getAvailableConnectionsCount());
        System.out.println("BorrowedConnectionsCount: " + pds.getBorrowedConnectionsCount());
        UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl
                .getUniversalConnectionPoolManager();
        UniversalConnectionPoolImpl pool = (UniversalConnectionPoolImpl) mgr.getConnectionPool("mypool");
        System.out.println("\t\t DATABASE INFO");
        System.out.println("\n\n" + pool.getDatabaseTopologyInfo());
        System.out.println("\n" + pool.getShardRoutingCacheInfo());
        System.out.println("-----------");
    }

    public void run() {
        int rc = 0;
        String s[] = new String[13];

        s[0] = String.valueOf(counter.incrementAndGet());


        //Instant.ofEpochMilli().toString();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date myDate = Date.from(Instant.now());
        String instantStr = sdf.format(myDate);
        int newSecond = Calendar.getInstance().get(Calendar.SECOND);

        s[1] = instantStr;
        s[2] = String.valueOf(thinkTime);

        if(!bSampleAcquistionComplete) {

            accountId = acctcounter.incrementAndGet();
            countryId = countryArray[(rand.nextInt(countryArray.length-1))];

        }
            OracleShardingKey shardKey = null;
        OracleShardingKey supershardKey = null;
        OracleConnection connection = null;

        try {
            //long starttime = System.currentTimeMillis();
            Instant starttime = Instant.now();
            double cursorTime = 0.0;
            double connectionTime = 0.0;
            //System.out.println("selected sharding key:"+ accountId);
            if (transparent_ds.equalsIgnoreCase("Y")) {
                connection = (OracleConnection) pds.getConnection();
            } else {
                shardKey = pds.createShardingKeyBuilder()
                        .subkey(accountId, OracleType.NUMBER)
                        .build();

                connection = (OracleConnection) pds.createConnectionBuilder()
                        .shardingKey(shardKey)
                        .build();
            }
            Instant endtime = Instant.now();
            connectionTime = Duration.between(starttime, endtime).toNanos() / 1000000;


            s[3] = String.valueOf(connectionTime);

            if(!bSampleAcquistionComplete) {
                try {
                    PreparedStatement pinsertacc = connection.prepareStatement(INSERTACCOUNTS);
                    pinsertacc.setString(1, countryId);
                    pinsertacc.setInt(2, accountId);
                    pinsertacc.setInt(3, accountId);
                    pinsertacc.setInt(4, randomInteger(10000,99999));
                    ResultSet rsInsertacc = pinsertacc.executeQuery();
                    connection.commit();
                    rsInsertacc.close();
                    pinsertacc.close();
                } catch (SQLException e) {
                    if (verbose.equalsIgnoreCase("debug")) {
                        throw new RuntimeException(e);
                    }
                }
            }
                synchronized (s) {
                doTransaction(connection, accountId, countryId, s);
                connection.close();
            }


        } catch (SQLException se) {
            if (bdisplayError.compareAndSet(false, true)) {
                if (verbose.equalsIgnoreCase("debug")) {
                    System.out.println("Error during connection creation to database: " + se.getMessage());
                }
            }
            if (verbose.equalsIgnoreCase("debug")) {
                se.printStackTrace();
            }
            errorcount.incrementAndGet();
            String oraCode = "";
            if(se.getMessage().indexOf("ORA") > 0) {
                oraCode = se.getMessage().substring(se.getMessage().indexOf("ORA"), se.getMessage().indexOf("ORA") + 9);
                oraCode = oraCode+":";
            }
            String errCode = oraCode+se.getMessage().substring(0,se.getMessage().length()>125?125:se.getMessage().length()-1);
            resultsSummary.putIfAbsent(errCode, new AtomicInteger());
            resultsSummary.get(errCode).incrementAndGet();
            writetoCsv(s);
            return;
        } catch (Exception e) {
            if (verbose.equalsIgnoreCase("debug")) {
                if (bdisplayError.compareAndSet(false, true)) {
                    System.out.println("Error during connection creation to database: " + e.getMessage());
                }

                System.out.println("Thread got Exception: " + e);
                e.printStackTrace();
            }
            errorcount.incrementAndGet();
            String oraCode = "";
            if(e.getMessage().indexOf("ORA") > 0) {
                oraCode = e.getMessage().substring(e.getMessage().indexOf("ORA"), e.getMessage().indexOf("ORA") + 9);
                oraCode = oraCode+":";
            }
            String errCode = oraCode+e.getMessage().substring(0,e.getMessage().length()>125?125:e.getMessage().length()-1);
            resultsSummary.putIfAbsent(errCode, new AtomicInteger());
            resultsSummary.get(errCode).incrementAndGet();
            writetoCsv(s);
            return;
        }
    }

    public void doTransaction(Connection conn, int account_id, String country_cd, String[] s) {
        try {
            if (thinkTime > 0 ) {
                RandomDataGenerator generator = new RandomDataGenerator(new Well19937c());
                double randomThink = generator.nextExponential(thinkTime);
                sleep((long) randomThink);
            }
            //Starting Info
            double pingTime = 0;
            Instant pingstarttime = Instant.now();
            conn.isValid(1);
            Instant pingendtime = Instant.now();
            pingTime = Duration.between(pingstarttime, pingendtime).toNanos() / 1000000;


            Instant starttime = Instant.now();
            int fundAmount = ThreadLocalRandom.current().nextInt(0, 999 + 1);

            int userId = 0;
            String sid = "";
            String instanceName = "";
            String cdb_pdb = "";
            boolean bVal = true;

            starttime = Instant.now();
            for (int iterCount = 0; iterCount < iterMax; iterCount++) {
                try {
                    PreparedStatement pupdate = conn.prepareStatement(UPDATE);
                    pupdate.setInt(1, fundAmount);
                    pupdate.setInt(2, account_id);
                    pupdate.setString(3, country_cd);
                    ResultSet rsUpdate = pupdate.executeQuery();

                    rsUpdate.close();
                    pupdate.close();
                    iterCount = iterMax;

                } catch (SQLException se) {
                    if (verbose.equalsIgnoreCase("debug")) {
                        if (!warningMessageSet.contains(se.getLocalizedMessage())) {
                            System.out.println("Thread got Exception during update statement:" + se.getLocalizedMessage());
                            warningMessageSet.add(se.getLocalizedMessage());
                        }
                    }

                    errorcount.incrementAndGet();
                    String oraCode = "";
                    if(se.getMessage().indexOf("ORA") > 0) {
                        oraCode = se.getMessage().substring(se.getMessage().indexOf("ORA"), se.getMessage().indexOf("ORA") + 9);
                        oraCode = oraCode+":";
                    }
                    String errCode = oraCode+se.getMessage().substring(0,se.getMessage().length()>125?125:se.getMessage().length()-1);
                    resultsSummary.putIfAbsent(errCode, new AtomicInteger());
                    resultsSummary.get(errCode).incrementAndGet();
                    writetoCsv(s);
                }
            }
            Instant endtime = Instant.now();

            double updateTime = Duration.between(starttime, endtime).toNanos() / 1000000;
           // s[13] = String.valueOf(updateTime);

            for (int iterCount = 0; iterCount < iterMax; iterCount++) {
                try {
                    PreparedStatement pselect = conn.prepareStatement(SELECT);

                    pselect.setInt(1, account_id);
                    pselect.setString(2, country_cd);

                    ResultSet rsSelect = pselect.executeQuery();


                    while (rsSelect.next()) {
                        userId = rsSelect.getInt(1);
                        instanceName = rsSelect.getString(2);
                        cdb_pdb = rsSelect.getString(3);
                        sid = String.valueOf(rsSelect.getInt(4));
                        shardMap.putIfAbsent(instanceName, new AtomicLong());
                        shardMap.get(instanceName).incrementAndGet();
                    }
                    shardSize = shardMap.size();
                    rsSelect.close();
                    pselect.close();
                    iterCount = iterMax;

                } catch (SQLException se) {
                    if (verbose.equalsIgnoreCase("debug")) {
                        if (!warningMessageSet.contains(se.getLocalizedMessage())) {
                            System.out.println("Thread got Exception during select statement: " + se.getLocalizedMessage());
                            warningMessageSet.add(se.getLocalizedMessage());
                        }
                    }

                    errorcount.incrementAndGet();
                    String oraCode = "";
                    if(se.getMessage().indexOf("ORA") > 0) {
                        oraCode = se.getMessage().substring(se.getMessage().indexOf("ORA"), se.getMessage().indexOf("ORA") + 9);
                        oraCode = oraCode+":";
                    }
                    String errCode = oraCode+se.getMessage().substring(0,se.getMessage().length()>125?125:se.getMessage().length()-1);
                    resultsSummary.putIfAbsent(errCode, new AtomicInteger());
                    resultsSummary.get(errCode).incrementAndGet();
                    writetoCsv(s);
                }
            }
            endtime = Instant.now();

            double selectTime = Duration.between(starttime, endtime).toNanos() / 1000000;
            s[4] = String.valueOf(selectTime);


            starttime = Instant.now();
            for (int iterCount = 0; iterCount < iterMax; iterCount++) {
                try {
                    PreparedStatement pinsert = conn.prepareStatement(INSERT);
                    pinsert.setString(1, country_cd);
                    pinsert.setInt(2, account_id);
                    pinsert.setInt(3, fundAmount);
                    ResultSet rsInsert = pinsert.executeQuery();

                    rsInsert.close();
                    pinsert.close();
                    iterCount = iterMax;

                } catch (SQLException se) {
                    if (verbose.equalsIgnoreCase("debug")) {
                        if (!warningMessageSet.contains(se.getLocalizedMessage())) {
                            System.out.println("Thread got Exception during insert statement:" + se.getLocalizedMessage());
                            warningMessageSet.add(se.getLocalizedMessage());
                        }
                    }

                    errorcount.incrementAndGet();
                    String oraCode = "";
                    if(se.getMessage().indexOf("ORA") > 0) {
                        oraCode = se.getMessage().substring(se.getMessage().indexOf("ORA"), se.getMessage().indexOf("ORA") + 9);
                        oraCode = oraCode+":";
                    }
                    String errCode = oraCode+se.getMessage().substring(0,se.getMessage().length()>125?125:se.getMessage().length()-1);
                    resultsSummary.putIfAbsent(errCode, new AtomicInteger());
                    resultsSummary.get(errCode).incrementAndGet();
                    writetoCsv(s);
                }
            }

            endtime = Instant.now();
            double insertTime = Duration.between(starttime, endtime).toNanos() / 1000000;
            s[5] = String.valueOf(insertTime+updateTime);

            starttime = Instant.now();
            conn.commit();

            endtime = Instant.now();
            double commitTime = Duration.between(starttime, endtime).toNanos() / 1000000;
            s[6] = String.valueOf(commitTime);


            double total_sql_time_ms = selectTime + insertTime + updateTime + commitTime;
            s[7] = String.valueOf(total_sql_time_ms);
            double total_time_ms = Double.parseDouble(s[3]) + total_sql_time_ms ;
            s[8] = String.valueOf(total_time_ms);

            s[9] = cdb_pdb;

            s[10] = instanceName;


            s[11] = driver_version;
            s[12] = database_version;
            synchronized (s) {
                responseTime.add(total_time_ms);
                writetoCsv(s);
            }
            resultsSummary.putIfAbsent(s[5], new AtomicInteger());
            resultsSummary.get(s[5]).incrementAndGet();

            //conn.close();

        } catch (SQLException sqex) {

            String oraCode = "";
            errorcount.incrementAndGet();
            if(sqex.getMessage().indexOf("ORA") > 0) {
                oraCode = sqex.getMessage().substring(sqex.getMessage().indexOf("ORA"), sqex.getMessage().indexOf("ORA") + 9);
                oraCode = oraCode+":";
            }
            String errCode = oraCode+sqex.getMessage().substring(0,sqex.getMessage().length()>125?125:sqex.getMessage().length()-1);
            resultsSummary.putIfAbsent(errCode, new AtomicInteger());
            resultsSummary.get(errCode).incrementAndGet();
            if (verbose.equalsIgnoreCase("debug")) {
                System.out.println("Transaction failed " + sqex.getMessage());
            }
        } catch (InterruptedException ix) {
            //nothing to do
        }
    }

    public synchronized long incrementAndGet() {
        idx++;
        return idx;
    }

    public void writetoCsv(String s[]) {
        csvWriter.writeNext(s);
    }

    public static void main(String[] args) throws Exception {
        try {
            if (args.length > 4) {
                url = "jdbc:oracle:thin:@" + args[0];
                userName = args[1];
                password = args[2];
                NUM_OF_THREADS = Integer.parseInt(args[3]);
                dur = Integer.parseInt(args[4]);
            } else {
                System.out.println("Application Usage: Accounts 'URL' 'UserName' 'Password' 'number of threads' 'duration' 'thinkTime(optional)' 'transparent datasource(optional)' 'debug'(optional)");
            }
            if (args.length == 6) {
                thinkTime = Integer.parseInt(args[5]);
            }
            if (args.length == 7) {
                transparent_ds = args[6];
            }
            if (args.length == 8) {
                verbose = args[7];
            }
            if (args.length == 9) {
                accountSamples = Integer.parseInt(args[8]);
            }
            if (verbose.equalsIgnoreCase("debug")) {
                System.out.println("URL Used: " + url + "\n User used: " + userName + "\nPassword used: " + password + "\nNumber of threads: " + NUM_OF_THREADS + "\n Duration of the test: " + dur);
            }
            if (verbose.equalsIgnoreCase("debug")) {
                System.out.println("Testing connections to URL = " + url);
            }
            //Getting the sample
            Instant runStartTimeSample = Instant.now();
            System.out.println("Starting the sampling");
            generateSample();

            Instant runendtimeSample = Instant.now();
            double duration = Duration.between(runStartTimeSample, runendtimeSample).toNanos() / 1000000;

            System.out.println("Duration to generate the sample in sec: " + duration / 1000);

            try (Connection conn = DriverManager.getConnection(url, userName, password);) {
                DatabaseMetaData meta = conn.getMetaData();
                driver_version = meta.getDriverVersion();
                database_version = String.valueOf(meta.getDatabaseMajorVersion());
                System.out.println("Succesfully established connection");
            } catch (Exception e) {
                System.out.println("Exception during connection, quiting the application");
                if (verbose.equalsIgnoreCase("debug")) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
            MAX_POOL_SIZE = NUM_OF_THREADS + 10;
            String formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now());
            String outFilename = "Maa_transacations_numberofthread" + NUM_OF_THREADS + "_duration" + dur + "_" + formattedDate + ".csv";
            Writer writer = Files.newBufferedWriter(Paths.get(outFilename));
            csvWriter = new CSVWriter(writer,
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            String[] header = {"transaction", "dttmUTC", "thinktime_ms", "connectiontime_ms", "selecttime_ms","dmltime_ms", "committime_ms", "total_ms", "totaltxn_ms", "database", "instance_name", "driver_version", "database_version"};
            NO_OF_COLUMNS = header.length;
            csvWriter.writeNext(header);
            if (verbose.equalsIgnoreCase("debug")) {
                System.out.println("Connection String Used for the transactions: " + url);
            }
            create_pool();

            //System.out.println("\t\t No of Users \t\tTransaction Count\tError Count\t\tTransaction per sec\t\t\tTransaction Details in Shards\t\t\t Connection details(available connections:borrowed connections");
            System.out.printf("%-22s%-22s%-22s%-22s%-22s%-22s%-22s\n", "Time", "No of Users", "Transaction Count", "Error Count", "Transaction per sec", "Transaction Details", "Connection details(available connections:borrowed connections");
            Timer timer = new Timer();
            timer.schedule(new TransactionsMaaTimer(), 0, 1000);
            //Only Insert

            Thread[] threadList = new Thread[NUM_OF_THREADS];
            Instant runStartTime = Instant.now();
            ExecutorService executor = Executors.newFixedThreadPool(NUM_OF_THREADS);
            while ((Instant.now().toEpochMilli() - runStartTime.toEpochMilli()) < (dur * 1000)) {
                 /*   for (int i = 0; i < NUM_OF_THREADS; i++) {
                        threadList[i] = new Transactions_Maa(i + 1);
                        threadList[i].start();
                    }
                    for (int i = 0; i < NUM_OF_THREADS; i++) {
                        threadList[i].join();
                    }*/
                Runnable worker = new Transactions_Maa(NUM_OF_THREADS);

                executor.execute(worker);
            }

            csvWriter.close();
            timer.cancel();

            System.out.println("\n Run Summary \n Number of Concurrent Users: " + NUM_OF_THREADS + "\n Duration of the test(in sec): " + dur + "\n Total number of transactions completed: " + counter + "\n Average Transactions Per Sec: " + counter.longValue() / dur);
            if (verbose.equalsIgnoreCase("debug")) {
                if (responseTime != null) {
                    Collections.sort(responseTime);


                    System.out.println("-------------------");

                    Map<Double, Long> counted = responseTime.stream()
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));


                    System.out.println("-------------------");

                    System.out.println("Response time details");

                    System.out.println("-------------------");

                    int totalResponses = responseTime.size();
                    System.out.println("Total Number of Transactions:" + totalResponses);

                    OptionalDouble average = responseTime
                            .stream()
                            .mapToDouble(a -> a)
                            .average();
                    double avgResponseTime = average.isPresent() ? average.getAsDouble() : 0;

                    System.out.println("Avergae resposne time in milli secs : " + avgResponseTime);

                    System.out.println("Response Time in millisecs" + "\t" + "Number of Transactions");
                    Object[] a = counted.entrySet().toArray();
                    Arrays.sort(a, new Comparator() {
                        public int compare(Object o1, Object o2) {
                            return ((Map.Entry<Double, Long>) o2).getValue()
                                    .compareTo(((Map.Entry<Double, Long>) o1).getValue());
                        }
                    });
                    for (Object e : a) {
                        System.out.println(((Map.Entry<Double, Long>) e).getKey() + " \t\t\t\t "
                                + ((Map.Entry<Double, Long>) e).getValue());
                    }

                    // Sort the occurrence map by response time
                    double[] percentiles = {1, 10, 20, 30, 40, 50, 60, 70, 80, 90};
                    Map<Double, Long> sortedMap = new TreeMap<>(counted);

                    int cumulativeCount = 0;
                    int currentPercentileIndex = 0;
                    for (Map.Entry<Double, Long> entry : sortedMap.entrySet()) {
                        double responseTime = entry.getKey();
                        long count = entry.getValue();
                        for (int i = 0; i < count; i++) {
                            cumulativeCount++;
                            double percentile = (cumulativeCount / (double) totalResponses) * 100.0;
                            //System.out.println("Current percentile Index"+currentPercentileIndex);
                            if (currentPercentileIndex < 10 && percentile > percentiles[currentPercentileIndex]) {
                                System.out.println(
                                        percentiles[currentPercentileIndex] + "-" + (percentiles[currentPercentileIndex] + 9) + " percentile: " + cumulativeCount
                                );

                                currentPercentileIndex++;
                            }

                        }
                    }
                }
            }
            System.out.println("Succesfully completed the test, check the result file at the location: " + outFilename);
            System.exit(1);
        } catch (SQLException ex) {
            if (verbose.equalsIgnoreCase("debug")) {
                throw new RuntimeException(ex);
            }
        } catch (IOException e) {
            if (verbose.equalsIgnoreCase("debug")) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void generateSample() {
        try {
            String[] countryArray =
                    {"AW", "CA", "CR", "HT", "MX", "US", "AR", "BO", "BR", "CL", "CO", "VE"};
            Map<String, Integer> resultMap = new HashMap<String, Integer>();
            int count = 0;
            OracleDataSourceImpl ods1 = null;
            ods1 = new OracleDataSourceImpl();
            ods1.setURL(url);
            ods1.setUser(userName);
            ods1.setPassword(password);

            ods1.setConnectionProperty("oracle.jdbc.useShardingDriverConnection", "true");
            Connection conn1 = ods1.getConnection();
            DatabaseMetaData meta = conn1.getMetaData();
            driver_version = meta.getDriverVersion();
            conn1.setAutoCommit(false);
            try (PreparedStatement psCount = conn1.prepareStatement(GETCOUNT);) {
                try (ResultSet rsCount = psCount.executeQuery()) {
                    while (rsCount.next()) {
                        int rowcount = rsCount.getInt(1);
                        if (rowcount < accountSamples) {
                            System.out.println("Not enough samples created yet, insert records to accounts table");
                            try (PreparedStatement psMax = conn1.prepareStatement(GETMAXACCOUNTID);) {
                                try (ResultSet rsMax = psMax.executeQuery()) {
                                    while (rsMax.next()) {
                                        int accountMax = rsMax.getInt(1);
                                        acctcounter.compareAndSet(0,accountMax);
                                    }
                                }
                            }
                            bSampleAcquistionComplete=false;
                           return;
                        }
                    }
                }
            }
                System.out.println("Connecting to catalog for getting the samples( this is a time consuming query based on your database size)");
                try (PreparedStatement ps = conn1.prepareStatement(GETSAMPLE);) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int acct_id = rs.getInt(1);
                            String country_cd = rs.getString(2);
                            String shardNo = rs.getString(3);
                            shardNo = "SHARD" + shardNo;
                            if (resultMap.containsKey(shardNo)) {
                                resultMap.put(shardNo, resultMap.get(shardNo) + 1);
                            } else {
                                resultMap.put(shardNo, count + 1);
                            }
                            acctIDShardMap.put(acct_id, shardNo);
                            accountdetails.put(acct_id, country_cd);
                            if (verbose.equalsIgnoreCase("debug")) {
                                // System.out.println("Sample customer Id added is: "+cust_id+ " Shard No:"+ String.valueOf(rs.getInt(2)));
                            }
                            acctList.add(acct_id);
                        }
                    }
                }
                if (verbose.equalsIgnoreCase("debug")) {
                    System.out.println("Here are the catalog details");
                    Statement statement = conn1.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT SYS_CONTEXT('userenv', 'instance_name') as instance_name"
                            + ", SYS_CONTEXT('userenv', 'server_host')" + " as server_host" + ", SYS_CONTEXT('userenv', 'service_name')"
                            + " as service_name" + ", SYS_CONTEXT('USERENV','db_unique_name')" + " as db_unique_name" + " from sys.dual");
                    resultSet.next();
                    System.out.println("instance_name : " + resultSet.getString("instance_name"));
                    System.out.println("server_host : " + resultSet.getString("server_host"));
                    serviceName = resultSet.getString("service_name");
                    System.out.println("service_name : " + serviceName);
                    System.out.println("db_unique_name : " + resultSet.getString("db_unique_name"));
                    resultSet.close();
                    statement.close();
                    System.out.println("\nUsing a sample size of 1000");
                    System.out.println("Sharding key distribution\n" + resultMap + "\n");
                }
            } catch (SQLException e) {
            if (verbose.equalsIgnoreCase("debug")) {
                throw new RuntimeException(e);
            }
            }

        }
}
class TransactionsMaaTimer extends TimerTask {
    static Timer timer;
    int count = 1;
    double lastcount=1;


    @Override
    public void run() {
        try {
            System.out.printf("%-22s%-22s%-22s%-22s%-22s%-22s%-22s%-22s\n",Instant.now().truncatedTo(ChronoUnit.SECONDS),Transactions_Maa.NUM_OF_THREADS,Transactions_Maa.counter,Transactions_Maa.errorcount,String.format("%.2f",(Transactions_Maa.counter.doubleValue()-lastcount)),Transactions_Maa.shardMap,"\t",Transactions_Maa.pds.getAvailableConnectionsCount()+":"+Transactions_Maa.pds.getBorrowedConnectionsCount());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        count = count+1;
        lastcount = Transactions_Maa.counter.doubleValue();

    }
}
