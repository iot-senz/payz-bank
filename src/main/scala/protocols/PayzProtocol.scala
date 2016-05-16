package protocols

object AccType extends Enumeration {
  type AccType = Value
  val SHOP, USER = Value
}

import AccType._

case class Acc(name: String, balance: Int, accType: AccType)

case class Trans(from_acc: String, to_acc: String, amount: Int, timestamp: String, status: String)