var app = angular.module("MicroBurn", ["lift-ng"]);
app.controller("ProjectController", ['$scope', function ($scope) {
  $scope.projectState = {
    sprints: []
  };

  $scope.$watch("projectState", function (projectState) {
    $scope.selectedSprint = projectState.sprints[projectState.sprints.length-1];
  });

  $scope.$watch("selectedSprint", function (sprint) {
    $("#chart").empty();

    if (typeof sprint === 'undefined')
      return;

    $.getJSON("history?sprintId=" + sprint.sprintId, function (response) {
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
  });

}]);


