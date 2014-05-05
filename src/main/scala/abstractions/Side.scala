package abstractions

sealed case class Side[A, B, D](id: A, values: B, strategy: MoveSupplier[A, D]) {
  def move(context: Context[A, B, _, D]): Move[A, D] = strategy(context)
  def apply(updatedValues: B) = copy(values = updatedValues)
}