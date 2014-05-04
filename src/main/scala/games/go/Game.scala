package games.go

import scala.collection.immutable.SortedSet
import scala.collection.immutable.TreeMap
import abstractions.Adversity
import abstractions.Context
import abstractions.Side
import abstractions.Sides
import components.Positions
import components.Positions._
import components.Cells
import Board.Symbols._
import scala.annotation.tailrec
import scala.util.Random
import scala.collection.immutable.Stack
import abstractions.MoveSupplier
import abstractions.ContextTrait
import abstractions.ContextTrait
import abstractions.Exploration

object Game {

  val debug = Array(
    "...XO.X",
    "...XOO.",
    "....XOO",
    ".....XO",
    ".......",
    ".......",
    "......."
  )

  type GoContext = Context[Char, Int, Board, Position]

  sealed case class GoMove(side: Char, data: Position) extends abstractions.Move[Char, Position] {
    override def toString = side + " -> " + (if (data == NullOption) "Pass" else data)
  }

  val NullOption = Position(Integer.MAX_VALUE, Integer.MAX_VALUE)

  val ai = Exploration(Evaluation)

  private def evaluateOptions(context: GoContext, depth: Int) = {
    val tmp = collection.mutable.Map[Long, Set[abstractions.Move[Char, Position]]]().withDefaultValue(Set())
    val legalMoves = context.options.toList
    val sortedLegalMoves = legalMoves.sortBy(_.data)
    val n = sortedLegalMoves.size
    if (n == 1) Map(0 -> legalMoves)
    else {
      var i = 0
      println("==============================================")
      sortedLegalMoves.foreach { move =>
        i += 1
        val ctx: ContextTrait[abstractions.Move[_, _]] = context.apply(move).asInstanceOf[ContextTrait[abstractions.Move[_, _]]]
        val key = ai(ctx, depth)
        println(i + "/" + n + " : " + move + " : " + key)
        tmp.update(key, tmp(key) + move)
      }
      val tmap = TreeMap()(math.Ordering.Long.reverse) ++ tmp
      tmap
    }
  }

  private sealed case class GoMoveSupplier1 extends MoveSupplier[Char, Position] {
    def apply(context: Context[Char, _, _, Position]) = {
      val ctx = context.asInstanceOf[GoContext]
      val legalMoves = context.options.toList
      var move = legalMoves(Random.nextInt(legalMoves.size))
      while (legalMoves.size > 1 && isNullMove(move)) {
        move = legalMoves(Random.nextInt(legalMoves.size))
      }
      move
    }
  }

  private sealed case class GoMoveSupplier2(depth: Int) extends MoveSupplier[Char, Position] {
    def apply(context: Context[Char, _, _, Position]) = {
      val ctx = context.asInstanceOf[GoContext]
      val map = evaluateOptions(ctx, depth)
      map.head._2.iterator.next
    }
  }

  private sealed case class GoMoveSupplier3 extends MoveSupplier[Char, Position] {
    def apply(context: Context[Char, _, _, Position]) = {
      val ctx = context.asInstanceOf[GoContext]
      val legalMoves = context.options.toList
      val sortedLegalMoves = legalMoves.sortBy(_.data)
      sortedLegalMoves.head
    }
  }

  private val sides = Sides(
    Adversity('O', 'X'),
    List(
      Side('O', 0, GoMoveSupplier2(2)),
      Side('X', 0, GoMoveSupplier2(5))
    )
  )

  private def isNullMove(move: abstractions.Move[Char, Position]) = move.data == NullOption

  private def isTerminalFunction(context: GoContext) =
    if (context.history.isEmpty) context.options == Set(GoMove(context.id, NullOption))
    else if (context.history.size == 1) false
    else if (isNullMove(context.lastMove)) isNullMove(context.history.head.lastMove)
    else false

  private def spaceMutation(move: abstractions.Move[Char, Position], space: Board) = {
    if (move.data == NullOption) space else space.play(move.data, move.side)
  }

  private def isLegal(move: abstractions.Move[Char, Position], context: GoContext) = {
    context.options.contains(move)
  }

  private def applicationFunction(context: GoContext, move: abstractions.Move[Char, Position]): GoContext = {
    if (move.side == context.id && !context.isTerminal && isLegal(move, context)) {
      val opponent = context.sides.nextTo(context.id)
      val spaceUpdate = spaceMutation(move, context.space)
      val numberOfCapturedStones = context.sideToPlay.values
      // TODO extract method in board
      val currentNumberOfStonesForopponents = context.space.cells.filterOthers(_._2 == opponent).size
      val nextNumberOfStonesForopponents = spaceUpdate.cells.filterOthers(_._2 == opponent).size
      val newCaptures = currentNumberOfStonesForopponents - nextNumberOfStonesForopponents
      val updatedSide = context.sideToPlay(numberOfCapturedStones + newCaptures)
      val updatedSides = context.sides.update(context.id, updatedSide)
      context.copy(id = opponent, sides = updatedSides, space = spaceUpdate)
    }
    else error("Illegal Move: " + move)
  }

  private def optionsFunction(context: GoContext): Stream[abstractions.Move[Char, Position]] = {
    val optionsFromBoard = context.space.layer(context.id).options
    if (optionsFromBoard.isEmpty) Stream(GoMove(context.id, Game.NullOption))
    else {
      val moves: Stream[abstractions.Move[Char, Position]] = optionsFromBoard.map(GoMove(context.id, _)).toStream
      val filteredMoves = moves.filterNot(move => context.setOfSpaces.contains(spaceMutation(move, context.space)))
      filteredMoves ++ Stream(GoMove(context.id, Game.NullOption))
    }
  }

  def main(args: Array[String]) {
    Main.main(args)
  }

  val context: GoContext = Context(
    sides.first, sides, Board(3, 4),
    null, Stack(), Set(),
    isTerminalFunction,
    applicationFunction,
    optionsFunction
  )

  /*
  sealed case class MyGoContext(ctx: GoContext) extends ContextTrait[GoMove] {
    def apply(move: GoMove) = MyGoContext(ctx.apply(move))
    def isTerminal: Boolean = true
    def options = Stream[GoMove]()
  }
  */

}