package abstractions

trait MoveSupplier[A, D] {

  def apply(context: AbstractContext[A, _, _, D]): Move[A, D]

}