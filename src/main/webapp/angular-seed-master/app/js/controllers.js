'use strict';
angular.module('myApp.controllers', [])
	.controller('Blokus', ['Blokus', 
        function(game) {
			game.populate();
			game.registerEvents();
			game.refresh();
		}
    ])
	.controller('Go', ['Go', 
        function(game) {
			game.populate();
			game.registerEvents();
			game.refresh();
		}
    ])    
    .controller('Rendering', ['$location', 'Blokus',
        function($location, game) {
    		var data = $location.search()['data']
    		var positions = game.parse(data.split(","));
        	game.populate();
        	game.resetCells();
        	game.render(positions);
		}
    ]);