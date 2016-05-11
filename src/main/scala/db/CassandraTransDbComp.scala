package db

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder._
import protocols.{Acc, Trans}

/**
 * Created by eranga on 2/2/16
 */
trait CassandraTransDbComp extends TransDbComp {

  this: SenzCassandraCluster =>

  val transDb = new CassandraTransDB

  class CassandraTransDB extends TransDb {

    def init() = {
      // query to create account
      val sqlCreateTableAccount = "CREATE TABLE IF NOT EXISTS acc(name TEXT PRIMARY KEY, balance Int);"

      // queries to create trans
      val sqlCreateTableTrans = "CREATE TABLE IF NOT EXISTS trans(from_acc TEXT, to_acc TEXT, amount INT, timestamp TEXT, status TEXT,PRIMARY KEY(from_account, timestamp));";

      val sqlCreateIndexTransStatus = "CREATE INDEX trans_status on trans(status);"
    }

    override def createAccount(account: Acc) = {
      // insert query
      val statement = QueryBuilder.insertInto("account")
        .value("name", account.name)
        .value("amount", account.branch)

      session.execute(statement)
    }

    override def getAccount(name: String): Option[Acc] = {
      // select query
      val selectStmt = select().all()
        .from("account")
        .where(QueryBuilder.eq("name", name))
        .limit(1)

      val resultSet = session.execute(selectStmt)
      val row = resultSet.one()

      if (row != null) Some(Acc(row.getString("name"), row.getString("amount")))
      else None
    }

    def transferMoney(trans: Trans) = {
      updateAccount(trans.from_acc, 50)
      updateAccount(trans.to_acc, 100)
    }

    override def createTrans(trans: Trans) = {
      // insert query
      val statement = QueryBuilder.insertInto("trans")
        .value("from_account", trans.from_acc)
        .value("to_account", trans.to_acc)
        .value("amount", trans.amount)
        .value("timestamp", trans.timestamp)
        .value("status", trans.status)

      session.execute(statement)
    }

    override def updateTrans(trans: Trans) = {
      // update query
      val updateStmt = QueryBuilder.update("trans")
        .`with`(set("status", trans.status))
        .where(QueryBuilder.eq("timestamp", trans.timestamp)).and(QueryBuilder.eq("name", trans.from_acc))

      session.execute(updateStmt)
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

  private def updateAccount(acc: String, amount: Int) = {
    val updateStmt = QueryBuilder.update("account")
      .`with`(set("amount", amount))
      .where(QueryBuilder.eq("account", acc))

    session.execute(updateStmt)
  }

}