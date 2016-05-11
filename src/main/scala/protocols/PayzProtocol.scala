package protocols

case class Acc(name: String, balance: Int)

case class Trans(from_acc: String, to_acc: String, amount: Int, timestamp: String, status: String)