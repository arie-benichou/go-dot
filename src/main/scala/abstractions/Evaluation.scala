package abstractions

trait Evaluation {

  def Success: Long
  def Failure: Long
  def None: Long
  def apply(context: ContextTrait[Move[_, _]]): Long
}