package games.go

import Game.GoContext
import abstractions.ContextTrait
import abstractions.Evaluation
import abstractions.Move

object Evaluation extends Evaluation {

  val Success = +999999999999L
  val Failure = -999999999999L
  val None = -1L

  private def captures(context: GoContext, side: Char) = context.side(side).values
  private def lands(context: GoContext, side: Char) = context.space.layer(side).lands.size
  private def freedom(context: GoContext, side: Char): Long = {
    val s = context.space.layer(side).strings
    val f = s.foldLeft(0)((sum, string) => sum + string.in.size * string.out.size)
    (context.space.rows * (f / (1.0 + s.size))).toInt
  }
  private def estimateSide(context: GoContext, side: Char): Long =
    ((1 + captures(context, side)) * freedom(context, side) * (1 + lands(context, side)))
  private def evaluateSide(context: GoContext, side: Char) =
    captures(context, side) + lands(context, side)
  private def estimate(context: GoContext /*, side: Char*/ ) =
    estimateSide(context, context.lastMove.side) - estimateSide(context, context.sides.nextTo(context.lastMove.side))
  private def evaluate(context: GoContext /*, side: Char*/ ): Long = {
    // TODO context.next
    val evaluation = evaluateSide(context, context.lastMove.side) - evaluateSide(context, context.sides.nextTo(context.lastMove.side))
    if (evaluation > 0) Success else if (evaluation < 0) Failure else None
  }

  def apply(ctx: ContextTrait[Move[_, _]]) = {
    val context = ctx.asInstanceOf[GoContext]
    val estimation = estimate(context)
    if (context.isTerminal) evaluate(context) + estimation
    else if (context.lastMove.data == Game.NullOption) {
      val e = evaluate(context)
      if (e <= None) e + estimation else estimation
    }
    else estimation
  }

}