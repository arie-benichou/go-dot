package abstractions

sealed case class Exploration(evaluation: Evaluation) {

  private def opponentReply(context: ContextTrait[Move[_, _]], depth: Int, ls: List[Move[_, _]], α: Long, β: Long): Long = {
    if (ls.isEmpty || β <= α) β
    else opponentReply(context, depth, ls.tail, α, Math.min(β, -apply(context(ls.head), depth - 1, -β, -α)))
  }

  def apply(context: ContextTrait[Move[_, _]], depth: Int, α: Long, β: Long): Long = {
    if (depth < 1 || context.isTerminal) evaluation(context)
    else {
      val legalMoves = context.options.toList
      // TODO define options ordering function in evaluation object
      // TODO ? define  inject options filtering function      
      //val sortedLegalMoves = legalMoves.sortBy(_.data)
      opponentReply(context, depth, legalMoves, α, β)
    }
  }

  def apply(context: ContextTrait[Move[_, _]], maxDepth: Int): Long = apply(context, maxDepth, evaluation.Failure, evaluation.Success)

}