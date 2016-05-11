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
      // query to create acc
      val sqlCreateTableAcc = "CREATE TABLE IF NOT EXISTS acc(name TEXT PRIMARY KEY, balance Int);"

      // queries to create trans
      val sqlCreateTableTrans = "CREATE TABLE IF NOT EXISTS trans(from_acc TEXT, to_acc TEXT, amount INT, timestamp TEXT, status TEXT,PRIMARY KEY(from_acc, timestamp));"

      val sqlCreateIndexTransStatus = "CREATE INDEX trans_status on trans(status);"
    }

    override def createAcc(acc: Acc) = {
      // insert query
      val statement = QueryBuilder.insertInto("acc")
        .value("name", acc.name)
        .value("balance", acc.balance)

      session.execute(statement)
    }

    override def getAcc(name: String): Option[Acc] = {
      // select query
      val selectStmt = select().all()
        .from("acc")
        .where(QueryBuilder.eq("name", name))
        .limit(1)

      val resultSet = session.execute(selectStmt)
      val row = resultSet.one()

      if (row != null) Some(Acc(row.getString("name"), row.getInt("balance")))
      else None
    }

    override def createTrans(trans: Trans) = {
      // insert query
      val statement = QueryBuilder.insertInto("trans")
        .value("from_acc", trans.from_acc)
        .value("to_acc", trans.to_acc)
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

    override def getTrans(from_acc: String, timestamp: String): Option[Trans] = {
      // select query
      val selectStmt = select().all()
        .from("trans")
        .where(QueryBuilder.eq("from_acc", from_acc)).and(QueryBuilder.eq("timestamp", timestamp))
        .limit(1)

      val resultSet = session.execute(selectStmt)
      val row = resultSet.one()

      if (row != null) Some(Trans(row.getString("from_acc"), row.getString("to_acc"), row.getInt("amount"), row.getString("timestamp"), row.getString("status")))
      else None
    }

    override def transferMoney(trans: Trans) = {
      updateAcc(trans.from_acc, 50)
      updateAcc(trans.to_acc, 100)
    }

    private def updateAcc(name: String, amount: Int) = {
      val updateStmt = QueryBuilder.update("acc")
        .`with`(set("amount", amount))
        .where(QueryBuilder.eq("name", name))

      session.execute(updateStmt)
    }

  }

}