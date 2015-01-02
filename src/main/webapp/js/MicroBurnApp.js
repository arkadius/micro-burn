var app = angular.module("MicroBurnApp", ["MicroBurnServices"]);

app.controller("ProjectCtrl", ['$scope', 'historySvc', function ($scope, historySvc) {
  $scope.projectState = {
    sprints: []
  };

  $scope.$watch("projectState", function (projectState) {
    $scope.selectedSprint = projectState.sprints[projectState.sprints.length-1];
  });

  var refreshChart = function () {
    historySvc.getHistory($scope.selectedSprint.id).then(function (history) {
      $scope.history = history;
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
              "<div id='chart'></div>" +
              "<div id='legend'></div>",
    link: function (scope, element, attrs) {
      scope.$watch("history", function(history){
        if (!history)
          return;

        element.attr("id", "sprint-chart-container");
        var chartElement = element.children("#chart");
        chartElement.empty();
        var yAxisElement = element.children("#y-axis");
        yAxisElement.empty();
        var legendElement = element.children("#legend");
        legendElement.empty();

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
          series: history.series
        });

        var toDays = function(millis) {
          return Math.floor((millis - history.startDate) / (1000 * 60 * 60 * 24))
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

        var legend = new Rickshaw.Graph.Legend({
          graph: graph,
          element: legendElement[0]
        });

        var highlighter = new Rickshaw.Graph.Behavior.Series.Toggle({
          graph: graph,
          legend: legend
        });

        var detail = new Rickshaw.Graph.HoverDetail({
          graph: graph,
          xFormatter: function(x) {
            var d = new Date(x),
              month = '' + (d.getMonth() + 1),
              day = '' + d.getDate(),
              year = d.getFullYear(),
              hour = '' + d.getHours(),
              min = '' + d.getMinutes();


            if (month.length < 2) month = '0' + month;
            if (day.length < 2) day = '0' + day;
            if (hour.length < 2) hour = '0' + hour;
            if (min.length < 2) min = '0' + min;

            var formattedDay = [year, month, day].join('-');
            var formattedTime = [hour, min].join(':');
            return [formattedDay, formattedTime].join("&nbsp;");
          },
          yFormatter: function(y) {
            return y;
          }
        });

        graph.render();
      });
    }
  };
});