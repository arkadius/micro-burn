var app = angular.module("MicroBurn", ["lift-ng"]);
app.filter('reverse', function() {
  return function(items) {
    if (typeof items === 'undefined')
      return items;
    else
      return items.slice().reverse();
  };
});

app.controller("ProjectController", ['$scope', function ($scope) {
  $scope.projectState = {};

  $scope.$watch("projectState", function (newValue) {
    console.log("projectState: ", newValue, " was changed");
  });
}]);


$.getJSON("history?sprintId=120", function (response) {
  console.log(response.series);

  var graph = new Rickshaw.Graph({
    element: document.querySelector("#chart"),
    renderer: "line",
    interpolation: "step-after",
    series: response.series
  });

  var time = new Rickshaw.Fixtures.Time();
  var xAxes = new Rickshaw.Graph.Axis.Time({
    graph: graph,
    timeUnit: time.unit('day')
  });

  var yAxes = new Rickshaw.Graph.Axis.Y({
    graph: graph
  });

  graph.render();

});