package games.go

import scala.collection.immutable.TreeMap
import scala.util.Random
import abstractions.AbstractContext
import abstractions.Adversity
import abstractions.Context
import abstractions.Exploration
import abstractions.MoveSupplier
import abstractions.Side
import abstractions.Sides
import components.Positions._

object Game {

  type GoMove = abstractions.Move[Char, Position]

  type GoContext = AbstractContext[Char, Int, Board, Position]

  sealed case class Move(side: Char, data: Position) extends abstractions.Move[Char, Position] {
    override def toString = side + " -> " + (if (data == NullOption) "Pass" else data)
  }

  val NullOption = Position(Integer.MAX_VALUE, Integer.MAX_VALUE)

  private def isNullMove(move: GoMove) = move.data == NullOption

  private def isLegalFunction(context: GoContext, move: GoMove) = {
    move.side == context.id && !context.isTerminal && context.options.contains(move)
  }

  private def isTerminalFunction(context: GoContext) =
    if (context.history.isEmpty) context.options == Set(Move(context.id, NullOption))
    else if (context.history.size == 1) false
    else if (isNullMove(context.lastMove)) isNullMove(context.history.head.lastMove)
    else false

  private def applicationFunction(context: GoContext, move: GoMove): GoContext = {
    if (move.data == NullOption)
      context.asInstanceOf[Context[Char, Int, Board, Position]].copy(id = context.next)
    else {
      val updatedBoard = context.space.play(move.data, move.side)
      val updatedCaptures = context.sideToPlay.values + updatedBoard.captured
      val updatedSides = context.sides.update(context.id, context.sideToPlay(updatedCaptures))
      context.asInstanceOf[Context[Char, Int, Board, Position]]
        .copy(id = context.next, sides = updatedSides, space = updatedBoard)
    }
  }

  private def optionsFunction(context: GoContext): Stream[GoMove] = {
    // TODO renommer en playablePositions
    val optionsFromBoard = context.space.layer(context.id).options
    if (optionsFromBoard.isEmpty) Stream(Move(context.id, Game.NullOption))
    else {
      val moves: Stream[GoMove] = optionsFromBoard.map(Move(context.id, _)).toStream
      val filteredMoves = moves.filterNot(move => context.setOfSpaces.contains(context.space.play(move.data, move.side)))
      filteredMoves ++ Stream(Move(context.id, Game.NullOption))
    }
  }

  private def reduce(context: GoContext) = {
    val positions = context.space.cells.filterOthers().foldLeft(Set[Position]()) { (s, p) =>
      s ++ (p * Directions.AllAround).filter(context.space.cells.get(_) == Board.Symbols.Space)
    }
    if (positions.isEmpty) Set(Position(context.space.rows / 2, context.space.columns / 2))
    else positions //-- context.space.layer(context.id).territory.locked
  }

  private def optionsFunctionAI(context: GoContext): Stream[GoMove] = {
    // TODO renommer en playablePositions
    val optionsFromBoard = context.space.layer(context.id).options
    if (optionsFromBoard.isEmpty) Stream(Move(context.id, Game.NullOption))
    else {
      val reducedOptions = optionsFromBoard.intersect(reduce(context))
      val moves: Stream[GoMove] = reducedOptions.map(Move(context.id, _)).toStream
      val filteredMoves = moves.filterNot(move => context.setOfSpaces.contains(context.space.play(move.data, move.side)))
      filteredMoves ++ Stream(Move(context.id, Game.NullOption))
    }
  }

  private val ai = Exploration(Evaluation)

  private def evaluateOptions(context: GoContext, depth: Int, optionsFunctionAI: (GoContext) => Stream[GoMove]): TreeMap[Long, Set[abstractions.Move[Char, Position]]] = {
    val tmp = collection.mutable.Map[Long, Set[GoMove]]().withDefaultValue(Set())
    val ctx = context.apply(optionsFunctionAI)
    val legalMoves = ctx.options.toList
    val sortedLegalMoves = legalMoves.sortBy(_.data)
    val n = sortedLegalMoves.size
    if (n == 1) TreeMap(0L -> legalMoves.toSet)
    else {
      var i = 0
      println("==============================================")
      sortedLegalMoves.foreach { move =>
        i += 1
        val key = ai(ctx.apply(move), depth)
        println(i + "/" + n + " : " + move + " : " + key)
        tmp.update(key, tmp(key) + move)
      }
      val tmap = TreeMap[Long, Set[GoMove]]()(math.Ordering.Long.reverse) ++ tmp
      tmap
    }
  }

  // TODO extract selection mechanism
  private sealed case class GoMoveSupplier1 extends MoveSupplier[Char, Position] {
    def apply(context: AbstractContext[Char, _, _, Position]) = {
      val legalMoves = context.options.toList
      var move = legalMoves(Random.nextInt(legalMoves.size))
      while (legalMoves.size > 1 && isNullMove(move)) {
        move = legalMoves(Random.nextInt(legalMoves.size))
      }
      move
    }
  }

  // TODO extract selection mechanism
  sealed case class GoMoveSupplier2(depth: Int) extends MoveSupplier[Char, Position] {
    def apply(context: AbstractContext[Char, _, _, Position]) = {
      val map = evaluateOptions(context.asInstanceOf[GoContext], depth, optionsFunctionAI)
      val (kills, others) = map.partition(_._1 >= Evaluation.Success)
      if (!kills.isEmpty && kills(kills.firstKey) == Set(Move(context.id, NullOption))) {
        //        println("I could win just by passing...")
        val fo = others.filter(e => e._1 > 0 && e._1 < Evaluation.Success)
        if (!fo.isEmpty) {
          val tmp = fo(fo.firstKey).head
          if (ai((context.asInstanceOf[GoContext])(tmp), depth + 1) >= fo.firstKey) {
            //            println("... but I will play !")
            fo(fo.firstKey).iterator.next
          }
          else map.head._2.iterator.next
        }
        else map.head._2.iterator.next
      }
      else map.head._2.iterator.next
    }
  }

  // TODO extract selection mechanism
  private sealed case class GoMoveSupplier3 extends MoveSupplier[Char, Position] {
    def apply(context: AbstractContext[Char, _, _, Position]) = {
      val legalMoves = context.options.toList
      val sortedLegalMoves = legalMoves.sortBy(_.data)
      sortedLegalMoves.head
    }
  }

  private val sides = Sides(Adversity('O', 'X'), List(
    Side('O', 0, GoMoveSupplier2(2)),
    Side('X', 0, GoMoveSupplier1())
  ))

  val context = Context(sides.first, sides, Board(7, 7), isLegalFunction, isTerminalFunction, applicationFunction, optionsFunction)

  def main(args: Array[String]) {
    Main.main(args)
  }

}