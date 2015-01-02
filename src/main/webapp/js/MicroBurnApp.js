var app = angular.module("MicroBurnApp", ["MicroBurnServices"]);

app.controller("ProjectCtrl", ['$scope', 'historySvc', function ($scope, historySvc) {
  $scope.projectState = {
    sprints: [],
    series: []
  };

  $scope.$watch("projectState", function (projectState) {
    $scope.selectedSprint = projectState.sprints[projectState.sprints.length-1];
  });

  var refreshChart = function () {
    historySvc.getHistory($scope.selectedSprint.id).then(function (history) {
      $scope.series = history.series;
    });
  };

  $scope.$watch("selectedSprint", function (sprint) {
    if (!sprint)
      return;
    refreshChart();
  });

  $scope.$on("boardStateChanged", function (event, sprintId) {
    if (sprintId == $scope.selectedSprint.id) {
      refreshChart();
    }
  });
}]);

app.directive('sprintChart', function () {
  return {
    template: "<div id='y-axis'></div>" +
              "<div id='chart'></div>",
    link: function (scope, element, attrs) {
      scope.$watch("series", function(series){
        if (!series)
          return;

        element.attr("id", "sprint-chart-container");
        var chartElement = element.children("#chart");
        chartElement.empty();
        var yAxisElement = element.children("#y-axis");
        yAxisElement.empty();

        var graph = new Rickshaw.Graph({
          element: chartElement[0],
          height: 500,
          min: "auto",
          padding: {
            right: 0.02,
            bottom: 0.05,
            top: 0.05
          },
          renderer: "line",
          interpolation: "step-after",
          series: series
        });

        var time = new Rickshaw.Fixtures.Time();
        var toDays = function(millis) {
          return Math.floor(millis / (1000 * 60 * 60 * 24))
        };
        var xAxes = new Rickshaw.Graph.Axis.X({
          graph: graph,
          tickFormat: toDays
        });

        var yAxes = new Rickshaw.Graph.Axis.Y({
          element: yAxisElement[0],
          graph: graph,
          orientation: "left"
        });

        graph.render();
      });
    }
  };
});