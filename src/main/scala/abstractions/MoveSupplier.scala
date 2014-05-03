package abstractions

trait MoveSupplier[A, B] {

  def apply(context: Context[A, _, _, B]): Move[A, B]

}