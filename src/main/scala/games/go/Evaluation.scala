package games.go

import games.go.Game._
import components.Positions.Position

object Evaluation {

  val success = +999999999999L
  val failure = -999999999999L
  val none = -1L

  private def opponent(character: Char) = if (character == 'O') 'X' else 'O'
  private def captures(context: GoContext, side: Char) = context.side(side).values
  private def lands(context: GoContext, side: Char) = context.space.layer(side).lands.size
  private def freedom(context: GoContext, side: Char): Long = {
    val s = context.space.layer(side).strings
    val f = s.foldLeft(0)((sum, string) => sum + string.in.size * string.out.size)
    (context.space.rows * (f / (1.0 + s.size))).toInt
  }

  //////////////////////////////////////////////////////////////////////////////

  // TODO extract to another Evaluation Object
  private def estimateSide(context: GoContext, side: Char): Long =
    ((1 + captures(context, side)) * freedom(context, side) * (1 + lands(context, side)))

  // TODO extract to another Evaluation Object
  private def evaluateSide(context: GoContext, side: Char) =
    captures(context, side) + lands(context, side)

  // TODO extract to another Evaluation Object
  private def estimate(context: GoContext, side: Char) =
    estimateSide(context, side) - estimateSide(context, opponent(side))

  // TODO extract to another Evaluation Object
  private def evaluate(context: GoContext, side: Char): Long = {
    val evaluation = evaluateSide(context, side) - evaluateSide(context, opponent(side))
    if (evaluation > 0) success else if (evaluation < 0) failure else none
  }

  ////////////////////////////////////////////////////////////////////////////////

  // TODO extract to another Evaluation Object
  def f(context: GoContext) =
    if (context.isTerminal) evaluate(context, context.lastMove.side) + estimateSide(context, context.lastMove.side)
    else {
      if (context.lastMove.data == NullOption) {
        val e = evaluate(context, context.lastMove.side)
        if (e < none) e + estimate(context, context.lastMove.side) else estimate(context, context.lastMove.side)
      }
      else estimate(context, context.lastMove.side)
    }

  def opp(newContext: GoContext, depth: Int, ls: List[abstractions.Move[Char, Position]], α: Long, β: Long): Long = {
    if (ls.isEmpty || β <= α) β
    else opp(newContext, depth, ls.tail, α, Math.min(β, -apply2(newContext(ls.head), depth - 1, -β, -α)))
  }

  // TODO extract to Exploration Object
  private def apply2(newContext: GoContext, depth: Int, α: Long, β: Long): Long = {
    if (depth < 1 || newContext.isTerminal) f(newContext)
    else {
      val legalMoves = newContext.options.toList
      val sortedLegalMoves = legalMoves.sortBy(_.data)
      //      sortedLegalMoves.foldLeft(Long.MaxValue) { (min, opponentMove) =>
      //        Math.min(min, -apply2(newContext(opponentMove), depth - 1))
      //      }
      opp(newContext, depth, sortedLegalMoves, α, β)
    }
  }

  def apply(context: GoContext, move: abstractions.Move[Char, Position], maxDepth: Int): Long =
    apply2(context(move), maxDepth, failure, success)

  def main(args: Array[String]) {
    Main.main(args)
  }

}