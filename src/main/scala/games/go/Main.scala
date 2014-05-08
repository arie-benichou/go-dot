package games.go

import scala.annotation.tailrec
import games.go.Game._
import components.Positions._

object Main {

  private def renderer(context: GoContext) {

    // TODO add score helpers
    val oc = context.sides.side('O').values
    val xc = context.sides.side('X').values
    val ol = 0 //context.space.layer('O').territory.closedPositions.size
    val xl = 0 //context.space.layer('X').territory.closedPositions.size

    println("==============================================")
    if (context.history.isEmpty) {
      println
      println("New Game")
    }
    else {
      val lastMove =
        if (context.lastMove.data != Game.NullOption)
          context.lastMove
        else if (context.history.head.options.size == 1)
          context.lastMove.side + " had to pass"
        else
          context.lastMove.side + " decided to pass"
      println
      println(lastMove)
    }

    println
    println(context.space)
    println
    println("O:(" + oc + ", " + ol + ")")
    println("X:(" + xc + ", " + xl + ")")
    println

    if (context.isTerminal) {
      println("==============================================")
      println
      println("Game is over !")

      val diff = (oc + ol) - (xc + xl)
      if (diff > 0)
        println("And the winner is player " + 'O')
      else if (diff < 0)
        println("And the winner is player " + 'X')
      else
        println("Tie game")

      println
      println("==============================================")
      println
    }
  }

  @tailrec
  private def run(context: GoContext)(implicit renderer: (GoContext) => Unit, limit: Int = -1): GoContext = {
    renderer(context)
    if (context.isTerminal || limit == 0) context else {
      val move = context.sideToPlay.strategy(context)
      run(context(move))(renderer, limit - 1)
    }
  }

  def main(args: Array[String]) {
    val t0 = System.currentTimeMillis()
    val terminalContext = run(Game.context)(renderer, -1)
    val t1 = System.currentTimeMillis()
    println("Game played in " + (t1 - t0) / 1000.0 + " seconds")
  }

}