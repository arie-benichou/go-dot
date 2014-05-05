package abstractions

trait AbstractEvaluation[A <: AbstractContext[_, _, _, _]] {
  def Success: Long
  def Failure: Long
  def None: Long
  def apply(context: A): Long
}