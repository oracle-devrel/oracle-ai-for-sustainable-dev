package oracleai.financial;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import oracle.jdbc.OracleConnection;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

public class Transactions_TrueCache extends Thread {
  static String url_primary_prefix = "jdbc:oracle:thin:@";
  private static int NUM_OF_THREADS = 3;

  private static PoolDataSource pds = null;
  private static int duration;
  static String url_primary = "172.20.1.195:1521/SALES";
  static String user = "c##transactions";
  static String password = "WElcomeHome123##";

  private static List<Integer> acctList = new ArrayList<Integer>();

  private static HashMap<Integer, String> accountdetails = new HashMap<Integer, String>();

  private String countryId = "";

  private int accountId = -1;

  Random rand = new Random();

  private static int sampleSize = 1;

  private static String bTrueCache = "false";

  private static Instant runStartTime =  Instant.now();
  private static AtomicInteger acctcounter = new AtomicInteger();

  private static AtomicInteger txncounter = new AtomicInteger();

  private static AtomicInteger rtxncounter = new AtomicInteger();

  private static AtomicInteger selectcounter = new AtomicInteger();

  private static AtomicInteger updatecounter = new AtomicInteger();

  private static AtomicInteger insertcounter = new AtomicInteger();

  private static AtomicInteger commitcounter = new AtomicInteger();
  private int thr_id = -1;

  public final String INFO = "SELECT   LOWER(d.db_unique_name || '_' || p.name) AS cdb_pdb\n" +
          "        ,i.instance_name\n" +
          "        ,i.status\n" +
          "        ,i.active_state\n" +
          "        ,i.database_type\n" +
          "FROM     v$pdbs      p,\n" +
          "         v$database  d,\n" +
          "         v$instance  i\n" +
          "ORDER BY i.instance_name";


  /*public final String SELECT = "SELECT user_id\n" +
          "   ,SYS_CONTEXT('USERENV', 'INSTANCE_NAME')\n" +
          "   ,(SELECT d.db_unique_name || '_' || p.name FROM v$pdbs p, v$database d)\n" +
          "   ,SYS_CONTEXT('USERENV', 'SID')\n" +
          "FROM   accounts\n" +
          "WHERE  account_id = ?\n" +
          "AND    country_cd = ? FOR UPDATE OF account_id";*/

  public final String SELECT = "SELECT user_id\n" +
          "          ,country_cd\n"  +
          "   ,SYS_CONTEXT('USERENV', 'INSTANCE_NAME')\n" +
          "   ,(SELECT d.db_unique_name || '_' || p.name FROM v$pdbs p, v$database d)\n" +
          "   ,SYS_CONTEXT('USERENV', 'SID')\n" +
          "FROM   accounts\n" +
          "WHERE  account_id = ?";

  public final String GETBALANCE = "SELECT user_id\n" +
          "          ,balance\n"  +
          "FROM   accounts\n" +
          "WHERE  account_id = ?";


  public final String GETCOUNTRY = "SELECT country_cd\n" +
          "FROM   accounts\n" +
          "WHERE  account_id = ?";

  public final String GETRECORD = "SELECT account_id\n" +
          "FROM   accounts,payments\n" +
          "WHERE account_id = ?";


  public final String INSERT  = "INSERT INTO payments\n" +
          "(\n" +
          "id\n" +
          ",country_cd\n" +
          ",account_id\n" +
          ",amount\n" +
          ",created_utc\n" +
          ")\n" +
          "VALUES\n" +
          "(\n" +
          "payment_id_seq.NEXTVAL\n"+
          ",?\n" +
          ",?\n" +
          ",?\n" +
          ",SYS_EXTRACT_UTC(SYSTIMESTAMP)\n" +
          ")";

  public final String UPDATE = "UPDATE accounts\n"+
          "SET    balance   = balance - ?\n" +
          ",last_modified_utc = SYSTIMESTAMP AT TIME ZONE 'UTC'\n" +
          "WHERE  account_id    = ?\n" +
          "AND    country_cd       = ?";

  public Transactions_TrueCache(int tid) throws SQLException
  {
    thr_id = tid;
  }


  public static void create_pool() throws SQLException
  {
    System.out.println(" \n\tBeginning of the transaction test!!!\n " );
    pds = PoolDataSourceFactory.getPoolDataSource();
    pds.setConnectionPoolName("Financial");
    pds.setUser(user);
    pds.setPassword(password);
    pds.setURL(url_primary);
    pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
    pds.setInitialPoolSize(NUM_OF_THREADS);
    pds.setMinPoolSize(1);
    pds.setMaxPoolSize(NUM_OF_THREADS*3);
    pds.setConnectionProperty("oracle.jdbc.useTrueCacheDriverConnection", "true");
  }

  public void run ()
  {
    int rc = 0;
    try
    {
      OracleConnection connection = (OracleConnection) pds.getConnection();
      connection.setAutoCommit(false);
      connection.setStatementCacheSize(60);
      doReadOnlyTransactionGetRecords(connection);
      doReadOnlyTransactionGetBalance(connection);
      doReadOnlyTransactionGetCountry(connection);
        doTransaction(connection);
      connection.close();

    } catch (Exception e)
    {
      System.out.println("Thread got Exception: " + e);
      e.printStackTrace();
      return;
    }
  }

  public void doReadOnlyTransactionGetBalance(Connection conn) {

    try {
      if (Boolean.valueOf(bTrueCache)) {
        conn.setReadOnly(true);
      }

      String instanceName = "";
      String cdb_pdb = "";
      String status = "";
      String state = "";
      String database_type = "";

        PreparedStatement pinfo = null;

        pinfo = conn.prepareStatement(INFO);


        ResultSet rsInfo = pinfo.executeQuery();
        selectcounter.incrementAndGet();
        while (rsInfo.next()) {
          cdb_pdb = rsInfo.getString(1);
          instanceName = rsInfo.getString(2);
          status = rsInfo.getString(3);
          state = rsInfo.getString(4);
          database_type = rsInfo.getString(5);

        }
        rsInfo.close();
        pinfo.close();

      PreparedStatement pselect = null;

      pselect = conn.prepareStatement(GETBALANCE);
      pselect.setInt(1, ThreadLocalRandom.current().nextInt(0, 9999999 + 1));

      ResultSet rsSelect = pselect.executeQuery();
      selectcounter.incrementAndGet();
      txncounter.incrementAndGet();
      rtxncounter.incrementAndGet();
      while (rsSelect.next()) {
        int userId = rsSelect.getInt(1);
        int balance = rsSelect.getInt(2);
      }
      rsSelect.close();
      pselect.close();
      if(conn.isReadOnly()) {
        conn.setReadOnly(false);
      }
    }catch(SQLException e){

    }

  }

  public void doReadOnlyTransactionGetCountry(Connection conn) {

    try {
      if (Boolean.valueOf(bTrueCache)) {
        conn.setReadOnly(true);
      }

      PreparedStatement pselect = null;

    pselect = conn.prepareStatement(GETBALANCE);
    pselect.setInt(1, ThreadLocalRandom.current().nextInt(0, 9999999 + 1));

    ResultSet rsSelect = pselect.executeQuery();
    selectcounter.incrementAndGet();
    txncounter.incrementAndGet();
      rtxncounter.incrementAndGet();
    while (rsSelect.next()) {
      String country = rsSelect.getString(1);
    }
    rsSelect.close();
    pselect.close();
      if(conn.isReadOnly()) {
        conn.setReadOnly(false);
      }
    }catch(SQLException e){

    }

  }

  public void doReadOnlyTransactionGetRecords(Connection conn) {

    try {
      if (Boolean.valueOf(bTrueCache)) {
        conn.setReadOnly(true);
      }

      PreparedStatement pselect = null;

      pselect = conn.prepareStatement(GETRECORD);
      pselect.setInt(1, ThreadLocalRandom.current().nextInt(0, 9999999 + 1));

      ResultSet rsSelect = pselect.executeQuery();
      selectcounter.incrementAndGet();
      txncounter.incrementAndGet();
      rtxncounter.incrementAndGet();
      while (rsSelect.next()) {
        int count = rsSelect.getInt(1);
      }
      rsSelect.close();
      pselect.close();
      if(conn.isReadOnly()) {
        conn.setReadOnly(false);
      }
    }catch(SQLException e){

    }

  }
  public void doTransaction(Connection conn) {



    double txnTime = 0;
    Instant txnstarttime = Instant.now();
    int fundAmount = ThreadLocalRandom.current().nextInt(0, 999 + 1);
    int userId = 0;
    String sid = "";
    String instanceName = "";
    String cdb_pdb = "";
    String status = "";
    String state = "";
    String database_type = "";
    boolean bVal = true;
    int index = acctcounter.incrementAndGet();
    int account_id = index;
    String country_cd = null;
    try {

      if (Boolean.valueOf(bTrueCache)) {
        conn.setReadOnly(true);
      }

      PreparedStatement pselect = null;

      pselect = conn.prepareStatement(SELECT);


      pselect.setInt(1, account_id);

      ResultSet rsSelect = pselect.executeQuery();
      selectcounter.incrementAndGet();
      while (rsSelect.next()) {
        userId = rsSelect.getInt(1);
        country_cd = rsSelect.getString(2);
        instanceName = rsSelect.getString(3);
        cdb_pdb = rsSelect.getString(4);
        sid = String.valueOf(rsSelect.getInt(5));
      }
      rsSelect.close();
      pselect.close();

    } catch (SQLException e) {

    }

    try {
      if(!conn.isReadOnly()) {

        PreparedStatement pupdate = conn.prepareStatement(UPDATE);
        pupdate.setInt(1, fundAmount);
        pupdate.setInt(2, account_id);
        pupdate.setString(3, country_cd);
        ResultSet rsUpdate = pupdate.executeQuery();
        updatecounter.incrementAndGet();
        rsUpdate.close();
        pupdate.close();
      }
    } catch (SQLException se) {

    }
    try {
      if(!conn.isReadOnly()) {

        PreparedStatement pinsert = conn.prepareStatement(INSERT);
        pinsert.setString(1, country_cd);
        pinsert.setInt(2, account_id);
        pinsert.setInt(3, fundAmount);
        ResultSet rsInsert = pinsert.executeQuery();
        insertcounter.incrementAndGet();
        rsInsert.close();
        pinsert.close();
      }
    } catch (SQLException se) {

    }

    try {
      if(!conn.isReadOnly()) {
        conn.commit();
        commitcounter.incrementAndGet();
      }
      if(conn.isReadOnly()){
        conn.setReadOnly(false);
      }

    } catch (SQLException e) {

    }

    Instant txnendtime = Instant.now();
    txnTime= Duration.between(txnstarttime, txnendtime).toNanos()/1000000;
    txncounter.incrementAndGet();
  }

  public static void main(String args[]) {
    try {
      if(args.length >5 ) {
        url_primary = args[0];
        url_primary = url_primary_prefix+url_primary;
        user = args[1];
        password = args[2];
        NUM_OF_THREADS = Integer.parseInt(args[3]);
        duration = Integer.parseInt(args[4]);
        bTrueCache = args[5];
      }else{
        System.out.println("Application Usage: Transactions_TrueCache 'URL' 'UserName' 'Password' 'number of threads' 'duration' 'use truecache'");
      }

      Thread[] threadList = new Thread[NUM_OF_THREADS];
      System.out.println("This is a demonstration of a transaction based framework");
      create_pool();
      if(Boolean.valueOf(bTrueCache)) {
        System.out.println("Using True cache for select statements now");
      }
      while ((Instant.now().toEpochMilli() - runStartTime.toEpochMilli()) < (duration * 1000)) {
        for (int i = 0; i < NUM_OF_THREADS; i++) {
          threadList[i] = new Transactions_TrueCache(i + 1);
          threadList[i].start();
        }
        for (int i = 0; i < NUM_OF_THREADS; i++) {
          threadList[i].join();
        }
      }
      System.out.println("\nResult Summary\n");
      System.out.println("Number of transactions completed: "+txncounter);
      System.out.println("Transactions per second: "+txncounter.get()/duration);
      System.out.println("Read Only Transactions per second: "+rtxncounter.get()/duration);
      System.out.println("\nNumber of selects completed: "+selectcounter);
      System.out.println("Number of updates completed: "+updatecounter);
      System.out.println("Number of inserts completed: "+insertcounter);
      System.out.println("\nNumber of commits completed: "+commitcounter);
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

}
