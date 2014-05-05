package abstractions

sealed case class Exploration[A, B, C, D](evaluation: AbstractEvaluation[AbstractContext[A, B, C, D]]) {

  private def opponentReply(context: AbstractContext[A, B, C, D], depth: Int, ls: List[Move[A, D]], α: Long, β: Long): Long = {
    if (ls.isEmpty || β <= α) β + evaluation(context)
    else opponentReply(context, depth, ls.tail, α, Math.min(β, -apply(context.apply(ls.head), depth - 1, -β, -α)))
  }

  def apply(context: AbstractContext[A, B, C, D], depth: Int, α: Long, β: Long): Long = {
    if (depth < 1 || context.isTerminal) evaluation(context)
    else {
      val legalMoves = context.options.toList
      // TODO define options ordering function in evaluation object
      // TODO ? define  inject options filtering function      
      //val sortedLegalMoves = legalMoves.sortBy(_.data)
      opponentReply(context, depth, legalMoves, α, β)
    }
  }

  def apply(context: AbstractContext[A, B, C, D], maxDepth: Int): Long = apply(context, maxDepth, evaluation.Failure, evaluation.Success)

}