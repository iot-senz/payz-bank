package db

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder._
import protocols.{AccType, Acc, Trans}

/**
 * Created by eranga on 2/2/16
 */
trait CassandraPayzDbComp extends PayzDbComp {

  this: PayzCassandraCluster =>

  val payzDb = new CassandraPayzDB

  object CassandraPayzDB {
    // can have minus balance for SHOP users
    val TransLimit: Int = -500
  }

  class CassandraPayzDB extends PayzDb {

    import CassandraPayzDB._

    def init() = {
      // query to create acc
      val sqlCreateTableAcc = "CREATE TABLE IF NOT EXISTS acc(name TEXT PRIMARY KEY, balance Int, acc_type TEXT);"

      // queries to create trans
      val sqlCreateTableTrans = "CREATE TABLE IF NOT EXISTS trans(t_id TEXT PRIMARY KEY, from_acc TEXT, to_acc TEXT, timestamp TEXT, amount INT, f_key TEXT, t_key TEXT, status TEXT);"

      val sqlCreateIndexTransStatus = "CREATE INDEX trans_status on trans(status);"
    }

    override def createAcc(acc: Acc) = {
      // insert query
      val statement = QueryBuilder.insertInto("acc")
        .value("name", acc.name)
        .value("balance", acc.balance)
        .value("acc_type", acc.accType.toString)

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

      if (row != null) Some(Acc(row.getString("name"), row.getInt("balance"), AccType.withName(row.getString("acc_type"))))
      else None
    }

    override def createTrans(trans: Trans) = {
      // insert query
      val statement = QueryBuilder.insertInto("trans")
        .value("t_id", trans.tId)
        .value("from_acc", trans.fromAcc)
        .value("to_acc", trans.toAcc)
        .value("timestamp", trans.timestamp)
        .value("amount", trans.amount)
        .value("f_key", trans.fKey)
        .value("t_key", trans.tKey)
        .value("status", trans.status)

      session.execute(statement)
    }

    override def updateTransStatus(trans: Trans) = {
      // update query
      val updateStmt = QueryBuilder.update("trans")
        .`with`(set("status", trans.status))
        .where(QueryBuilder.eq("t_id", trans.tId))

      session.execute(updateStmt)
    }

    override def getTrans(tId: String): Option[Trans] = {
      // select query
      val selectStmt = select().all()
        .from("trans")
        .where(QueryBuilder.eq("t_id", tId))
        .limit(1)

      val resultSet = session.execute(selectStmt)
      val row = resultSet.one()

      if (row != null) Some(Trans(row.getString("t_id"),
        row.getString("from_acc"),
        row.getString("to_acc"),
        row.getString("timestamp"),
        row.getInt("amount"),
        row.getString("f_key"),
        row.getString("t_key"),
        row.getString("status")))
      else None
    }

    override def transferMoney(trans: Trans) = {
      // find accounts
      val from_acc = getAcc(trans.fromAcc)
      val to_acc = getAcc(trans.toAcc)

      // validate from accounts
      from_acc match {
        case Some(Acc(name, balance, AccType.SHOP)) =>
          // top up
          // check for credit limit
          if (balance - trans.amount <= TransLimit) throw new Exception(s"Trans limit exceed of SHOP $name")
        case Some(Acc(name, balance, AccType.USER)) =>
          // transaction
          // check for balance
          if (balance < trans.amount) throw new Exception(s"No balance [$balance] to transfer in USER [$name]")
        case Some(_) =>
          // Un supported data
          throw new Exception("Unsupported account type")
        case None =>
          // No account, error
          throw new Exception("No from_acc")
      }

      // validate to account
      if (to_acc.isEmpty) throw new Exception("No to_acc")

      // came here means no error, so update accounts
      updateAcc(trans.fromAcc, from_acc.get.balance - trans.amount)
      updateAcc(trans.toAcc, to_acc.get.balance + trans.amount)
    }

    private def updateAcc(name: String, amount: Int) = {
      val updateStmt = QueryBuilder.update("acc")
        .`with`(set("balance", amount))
        .where(QueryBuilder.eq("name", name))

      session.execute(updateStmt)
    }

  }

}