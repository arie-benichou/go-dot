package abstractions

import scala.collection.immutable.Stack

sealed trait AbstractContext[A, B, C, D] {
  def id: A
  def next: A
  def sides: Sides[A, B, D]
  def sideToPlay: Side[A, B, D]

  def side(id: A): Side[A, B, D]
  def space: C
  def lastMove: Move[A, D]
  def history: Stack[AbstractContext[A, B, C, D]]
  def setOfSpaces: Set[C]
  def apply(move: Move[A, D]): AbstractContext[A, B, C, D]
  def apply(optionsFunction: AbstractContext[A, B, C, D] => Stream[Move[A, D]]): AbstractContext[A, B, C, D]
  def isTerminal: Boolean
  def options: Stream[Move[A, D]]
}

object Context {

  def apply[A, B, C, D](
    id: A, sides: Sides[A, B, D], space: C,
    isLegalFunction: (AbstractContext[A, B, C, D], Move[A, D]) => Boolean,
    isTerminalFunction: AbstractContext[A, B, C, D] => Boolean,
    applicationFunction: (AbstractContext[A, B, C, D], Move[A, D]) => AbstractContext[A, B, C, D],
    optionsFunction: AbstractContext[A, B, C, D] => Stream[Move[A, D]]): Context[A, B, C, D] =
    new Context(id, sides, space, null, Stack(), Set(), isLegalFunction, isTerminalFunction, applicationFunction, optionsFunction)

}

sealed case class Context[A, B, C, D](
  id: A, sides: Sides[A, B, D], space: C,
  lastMove: Move[A, D], history: Stack[AbstractContext[A, B, C, D]], setOfSpaces: Set[C],
  isLegalFunction: (AbstractContext[A, B, C, D], Move[A, D]) => Boolean,
  isTerminalFunction: AbstractContext[A, B, C, D] => Boolean,
  applicationFunction: (AbstractContext[A, B, C, D], Move[A, D]) => AbstractContext[A, B, C, D],
  optionsFunction: AbstractContext[A, B, C, D] => Stream[Move[A, D]])
  extends AbstractContext[A, B, C, D] {
  lazy val sideToPlay: Side[A, B, D] = side(id)
  lazy val isTerminal = isTerminalFunction(this)
  lazy val options = optionsFunction(this)
  lazy val next = this.sides.nextTo(this.id)
  def side(id: A): Side[A, B, D] = sides.side(id)
  def apply(newOptionsFunction: AbstractContext[A, B, C, D] => Stream[Move[A, D]]) = copy(optionsFunction = newOptionsFunction)
  def apply(move: Move[A, D]) = {
    if (this.isLegalFunction(this, move)) {
      val tmp = applicationFunction(this, move).asInstanceOf[Context[A, B, C, D]]
      tmp.copy(lastMove = move, history = history.push(this), setOfSpaces = setOfSpaces + this.space)
    }
    else error("Illegal Move: " + move)
  }
}