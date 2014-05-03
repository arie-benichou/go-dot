package abstractions

object Sides {

  def apply[A, B, C](adversity: Adversity[A], sides: List[Side[A, B, C]]): Sides[A, B, C] =
    new Sides(adversity, (adversity.ids zip sides).toMap)

}

sealed case class Sides[A, B, C](adversity: Adversity[A], sides: Map[A, Side[A, B, C]]) extends Iterable[(A, Side[A, B, C])] {

  val first: A = adversity.first

  def nextTo(id: A): A = adversity.successorOf(id)

  def side(id: A): Side[A, B, C] = sides.get(id).get

  def update(id: A, side: Side[A, B, C]): Sides[A, B, C] = copy(sides = sides.updated(id, side))

  def iterator = sides.iterator

}