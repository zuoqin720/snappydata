package io.snappydata.impl;

import com.gemstone.gemfire.distributed.internal.InternalDistributedSystem;
import com.gemstone.gemfire.distributed.internal.membership.InternalDistributedMember;
import com.pivotal.gemfirexd.internal.catalog.ExternalCatalog;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.hive.ExternalTableType;
import org.apache.thrift.TException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by kneeraj on 10/1/15.
 */
public class SnappyHiveCatalog implements ExternalCatalog {

  final private HiveMetaStoreClient hmc;

  final InternalDistributedMember thisMember;

//  public SnappyHiveCatalog() {
//
//    SparkContext ctx = SparkContext.getOrCreate();
//    assert ctx != null : "expected a non null spark context";
//    SparkConf conf = ctx.getConf();
//    assert conf != null : "expected a non null spark conf";
//
//    // initialize HiveMetaStoreClient
//    HiveConf metadataConf = new HiveConf();
//    if (conf.contains("gemfirexd.db.url")  && conf.contains("gemfirexd.db.driver")) {
//      metadataConf.setVar(HiveConf.ConfVars.METASTORECONNECTURLKEY,
//        conf.get("gemfirexd.db.url"));
//      metadataConf.setVar(HiveConf.ConfVars.METASTORE_CONNECTION_DRIVER,
//        conf.get("gemfirexd.db.driver"));
//    }
//    try {
//      this.hmc = new HiveMetaStoreClient(metadataConf);
//    } catch (MetaException me) {
//      throw new IllegalStateException(me);
//    }
//    thisMember = InternalDistributedSystem.getConnectedInstance().getDistributedMember();
//  }

  public SnappyHiveCatalog() {
    // initialize HiveMetaStoreClient
    String snappydataurl = "jdbc:snappydata:;locators=localhost[7777];persist-dd=false;";
    //String snappydataurl = "jdbc:snappydata:";

    HiveConf metadataConf = new HiveConf();
    metadataConf.setVar(HiveConf.ConfVars.METASTORECONNECTURLKEY,
        snappydataurl);
    metadataConf.setVar(HiveConf.ConfVars.METASTORE_CONNECTION_USER_NAME,
      "APP");
    metadataConf.setVar(HiveConf.ConfVars.METASTORE_CONNECTION_DRIVER,
      "com.pivotal.gemfirexd.jdbc.EmbeddedDriver");

    try {
      this.hmc = new HiveMetaStoreClient(metadataConf);
    } catch (MetaException me) {
      throw new IllegalStateException(me);
    }
    thisMember = InternalDistributedSystem.getConnectedInstance().getDistributedMember();
  }

  public boolean isColumnTable(String tableName) {
    try {
      String tableType = getType(tableName);
      if (tableType != null && tableType.equals(ExternalTableType.Columnar().toString())) {
        return true;
      }
    } catch (TException e) {
      throw new IllegalArgumentException(e);
    }
    return false;
  }

  public boolean isRowTable(String tableName) {
    try {
      String tableType = getType(tableName);
      System.out.println("KN: tableType = " + tableType);
      if (tableType != null && tableType.equals(ExternalTableType.Row().toString())) {
        return true;
      }
    } catch (TException e) {
      throw new IllegalArgumentException(e);
    }
    return false;
  }

  public boolean tableExists(String tableName) {
    try {
      this.hmc.getTable("", tableName);
    } catch (TException e) {
      return false;
    }
    return true;
  }

  // TODO: Will be implemented later when the serDe actually carries
  // this information
  public boolean hasComplexTypes(String tableName) {
    return false;
  }

  // TODO: Will be implemented later when the serDe actually carries
  // this information
  public boolean hasUserDefinedTypes(String tableName) {
    return false;
  }

  // TODO: Will be implemented later when the serDe actually carries
  // this information
  public boolean isSnappyUDF(String fnName) {
    return false;
  }

  public String getCatalogDescription() {
    return "Snappy Hive Catalog Client [" + thisMember + "]";
  }

  private String getType(String tableName) throws TException {
    List<String> tables = this.hmc.getAllTables("default");
    for (String s : tables) {
      System.out.println("table in default = " + s);
    }
    List<String> list = this.hmc.getAllDatabases();
    for (String s : list) {
      System.out.println("db = " + s);
    }
    Table t = this.hmc.getTable("default", tableName);
    String type = t.getTableType();
    System.out.println("KN: table type = " + type);
    Map<String, String> props = t.getParameters();//.getSd().getSerdeInfo().getParameters();
    Set<String> s = props.keySet();
    for(String p : s) {
      System.out.println("KN: Key = " + p + " val = " + props.get(p));
    }
    return t.getParameters().get("EXTERNAL");
  }
}