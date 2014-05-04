package abstractions

import scala.collection.immutable.Stack

trait ContextTrait[A <: Move[_, _]] {
  def apply(move: A): ContextTrait[A]
  def isTerminal: Boolean
  def options: Stream[A]
}

sealed case class Context[A, B, C, D](
  id: A, sides: Sides[A, B, D], space: C,
  lastMove: Move[A, D], history: Stack[Context[A, B, C, D]], setOfSpaces: Set[C],
  isTerminalFunction: Context[A, B, C, D] => Boolean,
  applicationFunction: (Context[A, B, C, D], Move[A, D]) => Context[A, B, C, D],
  optionsFunction: Context[A, B, C, D] => Stream[Move[A, D]])
  extends ContextTrait[Move[A, D]] {

  lazy val sideToPlay: Side[A, B, D] = side(id)
  lazy val isTerminal = isTerminalFunction(this)
  lazy val options = optionsFunction(this)

  def side(id: A): Side[A, B, D] = sides.side(id)

  def apply(move: Move[A, D]) = {
    applicationFunction(this, move).
      copy(lastMove = move, history = history.push(this), setOfSpaces = setOfSpaces + this.space)
  }

}