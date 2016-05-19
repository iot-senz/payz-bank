package protocols

object AccType extends Enumeration {
  type AccType = Value
  val SHOP, USER = Value
}

import AccType._

case class Acc(name: String, balance: Int, accType: AccType)

case class Trans(tId: String, fromAcc: String, toAcc: String, timestamp: String, amount: Int, fKey: String, tKey: String, status: String)