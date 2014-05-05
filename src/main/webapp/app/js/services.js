'use strict';
angular.module('myApp.services', []).
  service('Blokus', [function () {
	  return new Blokus.Game();
  }]).
  service('Go', [function () {
	  return new Go.Game();
  }]);