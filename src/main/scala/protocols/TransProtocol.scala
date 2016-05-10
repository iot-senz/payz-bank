package protocols

case class Agent(account: String, branch: String)

case class Trans(from_account: String, to_account: String, amount: Int, timestamp: String, status: String)