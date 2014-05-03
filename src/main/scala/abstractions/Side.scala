package abstractions

sealed case class Side[A, B, C](id: A, values: B, strategy: MoveSupplier[A, C]) {

  def move(context: Context[A, _, _, C]): Move[A, C] = strategy(context)
  def apply(updatedValues: B) = copy(values = updatedValues)

}