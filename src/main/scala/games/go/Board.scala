package games.go

import Board._
import Board.Symbols._
import Board.Layers._
import components.Cells
import components.Positions._
import scala.collection.immutable.TreeMap
import scala.collection.mutable.{ Map => MutableMap }
import scala.collection.immutable.SortedSet
import scala.annotation.tailrec
import components.Positions

object Board {

  object Symbols {
    val Black = 'O'
    val White = 'X'
    val Space = '.'
    val Undefined = '?'
  }

  private object Parser {
    private def cells(input: Array[String], initial: Char, undefined: Char) = {
      val rows = input.length
      val columns = if (rows == 0) 0 else input(0).length
      val data = Map[Position, Char]() ++ (for {
        row <- 0 until rows
        column <- 0 until columns
      } yield (Position(row, column), input(row).charAt(column)))
      (Cells(data, initial, undefined), rows, columns)
    }
    def apply(data: Array[String]) = cells(data, Space, Undefined)
  }

  object Chains {
    sealed case class Chain(in: Set[Position], out: Set[Position]) {
      override def toString = "\n  in:\n    " + this.in.mkString("\n    ") + "\n  out:\n    " + this.out.mkString("\n    ") + "\n"
    }
    sealed case class CellData(id: Int, in: Set[Position], out: Set[Position])
    private def updateMap(cells: Cells[Char], character: Char, position: Position, map: MutableMap[Position, CellData], tuple: (Int, Int)) = {
      val (max, id) = tuple
      val effectiveSides = (position * Directions.Sides).filterNot(cells.get(_) == Undefined)
      val connexions = effectiveSides.filter(cells.get(_) == character)
      val spaces = effectiveSides.filter(cells.get(_) == Space)
      val maxAndCurrentId = if (connexions.isEmpty) (max + 1, max + 1)
      else {
        val ids = connexions.map(map(_).id)
        if (ids == Set(0)) (max + 1, max + 1)
        else {
          val filteredIds = ids.filter(_ > 0)
          val min = filteredIds.min
          for {
            idToFix <- filteredIds.filterNot(_ == min)
            p <- map.filter(_._2.id == idToFix).keySet
          } {
            val data = map(p)
            map.put(p, CellData(min, data.in, data.out))
          }
          (max, min)
        }
      }
      map.put(position, CellData(maxAndCurrentId._2, connexions, spaces))
      maxAndCurrentId
    }
    private def buildMap(char: Char, cells: Cells[Char]): MutableMap[Position, CellData] = {
      val map = MutableMap[Position, CellData]().withDefaultValue(CellData(0, Set(), Set()))
      cells.positions.foldLeft((0, 0))((tuple, p) => if (cells.get(p) == char) updateMap(cells, char, p, map, tuple) else tuple)
      map
    }
    private def computeChains(cells: Cells[Char], character: Char) = {
      val map = buildMap(character, cells)
      val mapGroupedById = map.groupBy(e => e._2.id).mapValues(_.keySet)
      val rawChains = mapGroupedById.mapValues(p => (p, p.flatMap(p => map(p).out)))
      rawChains.map { e => val value = e._2; Chain(value._1.toSet, value._2.toSet) }.toSet
    }
    def apply(board: Board, character: Char) = computeChains(board.cells, character)
  }

  private object Territory {

    private def islands(board: Board, character: Char) = {
      val outsForOpponent = board.layer(opponent(character)).strings.flatMap(_.out)
      val outsForPlayer = board.layer(character).strings.flatMap(_.out)
      board.islands -- (outsForOpponent -- outsForPlayer)
    }

    private def capturable(board: Board, character: Char) = {
      val playerStrings = board.layer(character).strings
      board.islands(character).foldLeft(Set[Position]()) { (s, p) =>
        val outs = playerStrings.filter(_.out.contains(p)).flatMap(_.out)
        if (outs == Set(p)) s + p else s
      }
    }

    private def notCapturableYet(board: Board, character: Char) =
      board.layer(character).territory.islands -- board.layer(character).territory.capturable

    private def locked(board: Board, character: Char) = {
      board.layer(character).territory.notCapturableYet --
        (board.layer(opponent(character)).territory.notCapturableYet ++ board.layer(opponent(character)).territory.capturable)
    }

    //    private def closedPositions(board: Board, character: Char) = {
    //      val stringsForSpace = board.layer(Space).strings
    //      val opponentOuts = board.layer(opponent(character)).strings.flatMap(_.out)
    //      val lands = stringsForSpace.filter(s => s.in.intersect(opponentOuts).isEmpty).flatMap(_.in)
    //      if (lands.size == board.rows * board.columns) Set[Position]() else lands
    //    }

  }

  sealed case class Territory(board: Board, character: Char) {
    lazy val islands: Set[Position] = Territory.islands(board, character)
    //lazy val suicides: Set[Position] = Territory.suicides(board, character)
    lazy val capturable: Set[Position] = Territory.capturable(board, character)
    lazy val notCapturableYet: Set[Position] = Territory.notCapturableYet(board, character)
    lazy val locked: Set[Position] = Territory.locked(board, character)
    //lazy val closedPositions: Set[Position] = Territory.closedPositions(board, character)
  }

  private object Options {
    def apply(board: Board, character: Char) =
      board.spaces --
        (board.capturable(character) ++ board.locked(opponent(character))) ++
        board.capturable(opponent(character))
  }

  object Layers {
    sealed case class Layer(character: Char, board: Board) {
      lazy val strings = Chains(this.board, this.character)
      lazy val territory = Territory(this.board, this.character)
      lazy val options = Options(this.board, this.character)
    }
    def apply(board: Board) = Map(Black -> Layer(Black, board), White -> Layer(White, board), Space -> Layer(Space, board))
  }

  private object ToString {
    private def toArrayOfString(cells: Cells[Char]) = {
      val (rowMax, colMax) = (cells.positions.max.row, cells.positions.max.column)
      val data = Array.fill(rowMax + 1)(Undefined.toString * (colMax + 1))
      for (i <- 0 to rowMax) data.update(i, (0 to colMax).foldLeft("") { (str, j) => str + cells.get(Position(i, j)) })
      data
    }
    private def toString(data: Array[String]) = {
      val stringTopBottom = " " + "+" + "-" * (data(0).length) + "+" + "\n"
      val out0 = "  " + (0 until data(0).length).map(_ % 10).mkString + "\n" + stringTopBottom
      val string = data.foldLeft(out0)((out, in) => out + ((out.count(_ == '\n') - 2) % 10) + "|" + in + "|\n") + stringTopBottom
      string.dropRight(1)
    }
    def apply(cells: Cells[Char]): Array[String] = toArrayOfString(cells)
    def apply(board: Board): String = toString(apply(board.cells))
    def apply(data: Array[String]): String = toString(data)
  }

  private def opponent(character: Char) = if (character == Black) White else Black

  def apply(rows: Int, columns: Int): Board = {
    Board(Cells((Positions.from(Origin).to((rows - 1, columns - 1)))(Space), Space, Undefined), rows, columns)(0)
  }

  def apply(data: Array[String]): Board = {
    val (cells, rows, columns) = Parser(data) // TODO prendre en compte les captures ?
    Board(cells, rows, columns)(0)
  }

  def apply(n: Int): Board = apply(n, n)

}

sealed case class Board(cells: Cells[Char], rows: Int, columns: Int)(val captured: Int) {
  private lazy val spaces = this.cells.filterDefaults()
  lazy val islands = this.layer(Space).strings.filter(_.out.isEmpty).flatMap(_.in)
  private val layers: Map[Char, Layer] = Layers(this)
  def layer(character: Char) = this.layers(character)
  def islands(character: Char) = this.layers(character).territory.islands
  def capturable(character: Char) = this.layers(character).territory.capturable
  def notCapturableYet(character: Char) = this.layers(character).territory.notCapturableYet
  def locked(character: Char) = this.layers(character).territory.locked
  lazy val asArrayOfString = ToString(this.cells)
  override def toString = ToString(this.asArrayOfString)
  def play(position: Position, character: Char) = {
    val captures = this.layer(opponent(character)).strings.filter(_.out == Set(position)).flatMap(s => s.in)
    val updatedCells = this.cells.apply(captures.foldLeft(Map(position -> character))((map, p) => map + (p -> Space)))
    Board(updatedCells, rows, columns)(captures.size)
  }
}
