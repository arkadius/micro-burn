var app = angular.module("MicroBurnApp", ["MicroBurnServices", 'ngCookies']);

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

app.directive('sprintChart', ['$cookies', function ($cookies) {
  return {
    template: "<div id='y-axis'></div>" +
              "<div id='chart'></div>" +
              "<div id='legend'></div>",
    link: function (scope, element, attrs) {
      var series = [];
      var startDate = null;

      element.attr("id", "sprint-chart-container");

      var graph = new Rickshaw.Graph({
        element: element.children("#chart")[0],
        height: 600,
        min: "auto",
        padding: {
          right: 0.02,
          bottom: 0.05,
          top: 0.05
        },
        renderer: "line",
        unstack: true,
        interpolation: "step-after",
        series: series
      });

      var toDays = function(millis) {
        return Math.floor((millis - startDate) / (1000 * 60 * 60 * 24))
      };
      var xAxes = new Rickshaw.Graph.Axis.X({
        graph: graph,
        tickFormat: toDays
      });

      var yAxes = new Rickshaw.Graph.Axis.Y({
        element: element.children("#y-axis")[0],
        graph: graph,
        orientation: "left"
      });

      var legend = new Rickshaw.Graph.Legend({
        graph: graph,
        element: element.children("#legend")[0],
        naturalOrder: true
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

      scope.$watch("history", function(history) {
        if (!history)
          return;

        startDate = history.startDate;
        while (series.length) { // przepisujemy, bo wykres ma uchwyt do serii
          series.pop();
        }
        for (var i = 0; i < history.series.length; i++) {
          var column = history.series[i];
          if (column.name == 'Estimate') {
            column.color = "rgba(255, 0, 0, 0.9)";
          } else if (i == history.series.length - 1) { // DONE
            column.color = "rgba(0, 0, 0, 0.9)";
          } else {
            column.color = "rgba(0, 0, 255, 0." + i + ")";
          }
          if ($cookies["disabled_" + i]) {
            column.disabled = true;
          }
          series.push(column);
        }

        graph.render();
        legend.render();

        new Rickshaw.Graph.Behavior.Series.Toggle({ // to musi być wywyołand po przerenderowaniu legendy
          graph: graph,
          legend: legend
        });
        series.forEach(function (s, index) { // nadpisujemy metody dodane przez Toggle, żeby zapisywać sesję
          s.disable = function () {
            if (series.length <= 1) {
              throw('only one series left');
            }
            s.disabled = true;
            scope.$apply(function () {
              $cookies["disabled_" + index] = '1';
            });
          };
          s.enable = function () {
            s.disabled = false;
            scope.$apply(function () {
              delete $cookies["disabled_" + index];
            });
          };
        });
      });
    }
  };
}]);