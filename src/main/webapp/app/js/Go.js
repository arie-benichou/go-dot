var Go = Go || {};
/**
 * @constructor
 */
Go.Game = function() {
	this.colors = {
		"O" : "black",
		"X" : "white",
		"." : "space",
	};
	this.registerEvents();
	this.refresh();
};
Go.Game.prototype = {

	constructor : Go.Game,

	registerEvents : function() {

		$("#board").bind("mousedown", function(event) {
			event.preventDefault();
		});

		$("#_pass").bind("click", this.handleSubmit.bind(this));
		$("#ai").bind("click", this.handleAI.bind(this));
		$("#undo").bind("click", this.handleUndo.bind(this));
		$("#territory").bind("click", this.handleTerritory.bind(this));
		$("#options").bind("click", this.handleOptions.bind(this));

	},

	refresh : function() {
		$.ajax({
			url : "/go/context",
			success : this.update.bind(this)
		});
	},

	cell : function(i, j) {
		return $("#" + ("r" + i + "c" + j));
	},

	// TODO extract to ContextRenderer
	renderBoard : function(context) {
		var board = $("#board");
		board.html("")
		for ( var i in context.space) {
			var row = context.space[i]
			board.append("<tr></tr>");
			var tr = $("#board tr:last-child");
			for ( var j in row) {
				tr.append("<td id='" + ("r" + (i) + "c" + j) + "'>?</td>");
				var cell = this.cell(i, j);
				cell.html("");
				cell.attr('class', this.colors[row[j]])
			}
		}
	},

	// TODO extract to ContextRenderer
	renderOptions : function(context) {
		$("td").unbind("click");
		var color = context.side == "O" ? "black" : "white";
		context.options.map(function(option) {
			var data = option.split(':')
			var row = parseInt(data[0]);
			var column = parseInt(data[1]);
			var cell = this.cell(row, column);
			cell.attr("style", "color:" + color)
			cell.html("o");
			cell.bind("click", this.handleSubmit.bind(this));
		}.bind(this))
	},

	// TODO extract to ContextRenderer
	renderTerritory : function(context) {
		$("td").unbind("click");
		$("td").html("")
		var color = context.side == "O" ? "black" : "white"
		context.lands.map(function(position) {
			var row = position.row;
			var column = position.column;
			var cell = this.cell(row, column);
			cell.attr("style", "color:" + color)
			cell.html("x");
		}.bind(this))
	},

	update : function(context) {
		// TODO extract to ContextRenderer
		this.ctx = context;

		this.renderBoard(context);
		this.renderOptions(context);
		
		$("#data").html("");
		// TODO à revoir
		if (context["last-move"] == "-2147483561:2147483647" || context["last-move"] == "-2147483570:2147483647") {
			$("#data").append((context["side"] == "X" ? "Black" : "White") + " has passed<br/>");
		}
		if (context["is-over"] == "true")
			$("#data").append("Game Over !");
		else
			$("#data").append((context["side"] == "O" ? "Black" : "White") + " has to play");
	},

	handleSubmit : function(event) {
		$("td").html("")
		var target = $(event.target).attr("id");
		var move = this.ctx.side + target.substr(1).split("c").join(":");
		$.ajax({
			url : "/go/play/" + move,
			method : "get",
			success : this.update.bind(this),
			error : function() {
				alert("error");
			}
		});
	},

	handleAI : function(event) {
		$("td").html("")
		$.ajax({
			url : "/go/ai",
			method : "get",
			success : this.update.bind(this),
			error : function() {
				alert("error");
			}
		});
	},

	handleUndo : function(event) {
		$.ajax({
			url : "/go/undo",
			method : "get",
			success : this.update.bind(this),
			error : function() {
				alert("error");
			}
		});
	},

	handleTerritory : function(event) {
			this.renderBoard(this.ctx);
			this.renderTerritory(this.ctx);
	},
	
	handleOptions : function(event) {
			this.renderBoard(this.ctx);
			this.renderOptions(this.ctx);
	},

};