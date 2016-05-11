package protocols

case class Acc(name: String, branch: String)

case class Trans(from_acc: String, to_acc: String, amount: Int, timestamp: String, status: String)