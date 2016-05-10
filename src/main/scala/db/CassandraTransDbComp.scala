package db

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder._
import protocols.{Account, Trans}

/**
 * Created by eranga on 2/2/16
 */
trait CassandraTransDbComp extends TransDbComp {

  this: SenzCassandraCluster =>

  val transDb = new CassandraTransDB

  class CassandraTransDB extends TransDb {

    def init() = {
      // query to create account
      val sqlCreateTableAccount = "CREATE TABLE IF NOT EXISTS account (name TEXT PRIMARY KEY, balance TEXT);"

      // queries to create trans
      val sqlCreateTableTrans = "CREATE TABLE IF NOT EXISTS trans(from_account TEXT, to_account TEXT, amount INT, timestamp TEXT, status TEXT,PRIMARY KEY(from_account, timestamp));";

      val sqlCreateIndexTransStatus = "CREATE INDEX trans_status on trans(status);"
    }

    override def createAccount(account: Account) = {
      // insert query
      val statement = QueryBuilder.insertInto("account")
        .value("name", account.name)
        .value("amount", account.branch)

      session.execute(statement)
    }

    override def getAccount(name: String): Option[Account] = {
      // select query
      val selectStmt = select().all()
        .from("account")
        .where(QueryBuilder.eq("name", name))
        .limit(1)

      val resultSet = session.execute(selectStmt)
      val row = resultSet.one()

      if (row != null) Some(Account(row.getString("name"), row.getString("amount")))
      else None
    }

    override def createTrans(trans: Trans) = {
      // insert query
      val statement = QueryBuilder.insertInto("trans")
        .value("from_account", trans.from_account)
        .value("to_account", trans.to_account)
        .value("amount", trans.amount)
        .value("timestamp", trans.timestamp)
        .value("status", trans.status)

      session.execute(statement)
    }

    override def updateTrans(trans: Trans) = {
      // update query
      val statement = QueryBuilder.update("trans")
        .`with`(set("status", trans.status))
        .where(QueryBuilder.eq("timestamp", trans.timestamp)).and(QueryBuilder.eq("name", trans.from_account))

      session.execute(statement)
    }

    override def getTrans(agent: String, timestamp: String): Option[Trans] = {
      // select query
      val selectStmt = select().all()
        .from("trans")
        .where(QueryBuilder.eq("agent", agent)).and(QueryBuilder.eq("timestamp", timestamp))
        .limit(1)

      val resultSet = session.execute(selectStmt)
      val row = resultSet.one()

      if (row != null) Some(Trans(row.getString("from_account"), row.getString("to_account"), row.getInt("amount"), row.getString("timestamp"), row.getString("status")))
      else None
    }
  }

}