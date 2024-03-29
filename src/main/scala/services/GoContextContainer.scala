package services

import org.json4s.DefaultFormats
import org.json4s.Formats
import org.scalatra.json.JValueResult
import org.scalatra.json.JacksonJsonSupport

import components.Positions.Position
import games.go.Game
import games.go.Game._
import GoContextContainer._

object GoContextContainer {

  private def parseIncomingMove(incomingMove: String) = {
    val side = incomingMove.head
    val tail = incomingMove.tail
    val position =
      if (tail == "pass") Game.NullOption else {
        val data = tail.split(':').map(Integer.parseInt)
        Position(data(0), data(1))
      }
    Move(side, position)
  }

  private def moveToString(move: GoMove) = if (move == null) "" else move.side.toString().charAt(0) + move.data.row + ":" + move.data.column

  private def pathToString(ctx: GoContext) = {
    val stack = ctx.history.map { ctx => moveToString(ctx.lastMove) }
    (if (ctx.lastMove != null) stack.push(moveToString(ctx.lastMove)) else stack).mkString(",")
  }

}

class GoContextContainer extends GameContextContainer with JacksonJsonSupport with JValueResult {

  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  var context: GoContext = Game.context

  get("/go/context") {
    val ctx = context
    Map(
      "side" -> ctx.id.toString,
      "is-over" -> ctx.isTerminal.toString,
      "space" -> ctx.space.asArrayOfString,
      "last-move" -> moveToString(ctx.lastMove),
      "options" -> (if (ctx.isTerminal) Set() else ctx.space.layer(ctx.id).options.map(p => p.row + ":" + p.column)),
      //"lands" -> ctx.space.layer(ctx.id).territory.closedPositions
      "lands" -> ctx.space.layer(ctx.id).territory.notCapturableYet
    //,"scores" -> ctx.sides.map(e => (e._1.toString, Game.score(ctx, e._1))).toMap
    )
  }

  get("/go/play/:move") {
    try {
      val ctx = context
      val move = parseIncomingMove(params("move"))
      this.synchronized { context = ctx.apply(move) }
    }
    catch {
      case e: Exception => {
        val path = pathToString(context)
        println
        println("################################ Illegal Instruction ################################")
        println
        println("query              : " + params("move"))
        println("side to play       : " + context.id)
        println("message            : " + e)
        println
        println(context.space)
        println("#####################################################################################")
        println
        //println("http://localhost:8080/angular-seed-master/app/#/rendering?data=" + path)
      }
    }
    redirect("/go/context")
  }

  get("/go/ai") {
    val ctx = context;
    try {
      if (!ctx.isTerminal) {
        this.synchronized { context = ctx(ctx.sideToPlay.strategy.apply(ctx)) }
      }
    }
    catch {
      case e: Exception => {
        val path = pathToString(ctx)
        println
        println("################################ Illegal Instruction ################################")
        println
        println("query              : " + params("move"))
        println("side to play       : " + ctx.id)
        println("message            : " + e)
        println
        println(ctx.space)
        println("#####################################################################################")
        println
        //println("http://localhost:8080/angular-seed-master/app/#/rendering?data=" + path)
      }
    }
    redirect("/go/context")
  }

  get("/go/undo") {
    val ctx = context;
    if (!ctx.history.isEmpty) this.synchronized { context = ctx.history.head }
    redirect("/go/context")
  }

  get("/go/reset") {
    val ctx = context;
    this.synchronized { context = Game.context }
    redirect("/go/context")
  }

}
