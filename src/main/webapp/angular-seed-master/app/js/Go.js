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
};
Go.Game.prototype = {

	constructor : Go.Game,

	populate : function() {
		var rows = 9
		var columns = 9
		var board = $("#board");
		for ( var i = 0; i < rows; ++i) {
			board.append("<tr></tr>");
			var row = $("#board tr:nth-child(" + (i + 1) + ")");
			for ( var j = 0; j < columns; ++j)
				row.append("<td id='" + ("r" + i + "c" + j) + "'></td>");
		}
	},

	registerEvents : function() {
		$("#pass").bind("click", this.handleSubmit.bind(this));
		$("#ai").bind("click", this.handleAI.bind(this));
		$("#undo").bind("click", this.handleUndo.bind(this));
		$("#board").bind("mousedown", function(event) {
			event.preventDefault();
		});
	},

	refresh : function() {
		$.ajax({
			url : "/go/context",
			success : $.proxy(this.update, this)
		});
	},

	cell : function(i, j) {
		return $("#" + ("r" + i + "c" + j));
	},

	// TODO extract to ContextRenderer
	renderBoard : function(context) {
		for (i in context.space) {
			var row = context.space[i]
			for (j in row) {
				var cell = this.cell(i, j);
				cell.unbind("click");
				cell.html("");
				cell.attr('class', this.colors[row[j]])
			}
		}
	},

	// TODO extract to ContextRenderer
	renderOptions : function(context) {
		context.options.map(function(option) {
			var data = option.split(':')
			var row = parseInt(data[0]);
			var column = parseInt(data[1]);
			var cell = this.cell(row, column);
			cell.html("o");
			cell.data("move", context.side + row + ":" + column);
			cell.bind("click", this.handleSubmit.bind(this));
		}.bind(this))
	},
	
	update : function(context) {
		// TODO extract to ContextRenderer
		this.ctx = context;
		this.renderBoard(context);
		this.renderOptions(context);
		console.debug(context["is-over"]);
		if (context["is-over"] == "true")
			alert("Game Over");
	},

	handleSubmit : function(event) {
		var move = $(event.target).data("move");
		if (move == undefined)
			move = this.ctx.side + "";
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
	}	

};