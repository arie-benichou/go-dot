'use strict';
angular.module('myApp', [
  'ngRoute',
  'myApp.filters',
  'myApp.services',
  'myApp.directives',
  'myApp.controllers'
]).
config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/go', {templateUrl: 'partials/new-game.html', controller: 'Go'});
  $routeProvider.when('/blokus', {templateUrl: 'partials/new-game.html', controller: 'Go'});
  $routeProvider.when('/rendering', {templateUrl: 'partials/rendering.html', controller: 'Rendering'});
  $routeProvider.otherwise({redirectTo: '/'});
}]);