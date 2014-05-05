package games.go

import Game.GoContext
import abstractions.AbstractEvaluation

object Evaluation extends AbstractEvaluation[GoContext] {

  val Success = +9999999999991L
  val Failure = -9999999999991L
  val None = -1L

  private def captures(context: GoContext, side: Char) = context.side(side).values

  private def lands(context: GoContext, side: Char) = context.space.layer(side).lands.size

  private def freedom(context: GoContext, side: Char): Long = {
    val s = context.space.layer(side).strings
    val f = s.foldLeft(0)((sum, string) => sum + string.in.size * string.out.size)
    (context.space.rows * context.space.columns * (f / (1.0 + s.size))).toInt
  }

  private def estimateSide(context: GoContext, side: Char): Long =
    context.space.rows * context.space.columns * (1 + lands(context, side)) * (1 + captures(context, side)) + freedom(context, side)

  private def evaluateSide(context: GoContext, side: Char): Long =
    captures(context, side) + lands(context, side)

  private def estimate(context: GoContext): Long =
    estimateSide(context, context.lastMove.side) - estimateSide(context, context.id)

  private def evaluate(context: GoContext): Long = {
    val evaluation = evaluateSide(context, context.lastMove.side) - evaluateSide(context, context.id)
    if (evaluation > 0) Success else if (evaluation < 0) Failure else None
  }

  def apply(context: GoContext): Long = {
    val estimation = estimate(context)
    if (context.isTerminal) evaluate(context) + estimation
    else if (context.lastMove.data == Game.NullOption) {
      val e = evaluate(context)
      if (e <= None) e + estimation else estimation
    }
    else estimation
  }

}