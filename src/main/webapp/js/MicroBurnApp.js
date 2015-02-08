window.dateFormat = 'Y-m-d H:i';

function wrapServiceCall(call) {
  $("#cover").show();
  var callResult = call();
  callResult.catch(function (msg) {
    window.alert("Error: " + msg);
  });
  callResult.then(function () {
    $("#cover").hide();
  });
}

var app = angular.module("MicroBurnApp", ["MicroBurnServices", 'ngCookies']);

app.controller("ProjectCtrl", ['$scope', 'historySvc', 'scrumSimulatorSvc', function ($scope, historySvc, scrumSimulatorSvc) {
  window.scrumSimulatorSvc = scrumSimulatorSvc;

  $scope.projectState = null;
  $scope.sprintsOrdered = [];
  $scope.existsActiveSprint = false;

  $scope.editedSprint = {
    name: "",
    start: "",
    end: ""
  };

  $scope.editMode = false;

  $scope.$watch("projectState", function (projectState) {
    if (projectState) {
      var existsActiveSprint = false;
      for (var i = 0; i < projectState.sprints.length; i++) {
        var sprint = projectState.sprints[i];
        if (sprint.details.isActive) {
          sprint.details.formattedName = sprint.details.name;
          sprint.order = "1" + sprint.details.start; // active on top of list
          existsActiveSprint = true;
        } else {
          sprint.details.formattedName = sprint.details.name + " (inactive)";
          sprint.order = "0" + sprint.details.start;
        }
      }
      $scope.existsActiveSprint = existsActiveSprint;
      projectState.sprints.sort(function (f, s) {
        return s.order.localeCompare(f.order);
      });
      $scope.sprintsOrdered = projectState.sprints;
      $scope.selectedSprint = $scope.sprintsOrdered[0];
    } else {
      $scope.selectedSprint = null;
    }
    disableModes();
  });

  var refreshChart = function () {
    if ($scope.selectedSprint) {
      historySvc.getHistory($scope.selectedSprint.id).then(function (history) {
        $scope.history = history;
      });
    } else {
      $scope.history = null;
    }
  };

  $scope.$watch("selectedSprint", function (sprint) {
    if (sprint) {
      $scope.editedSprint = {
        name: "",
        start: new Date($scope.selectedSprint.details.start).dateFormat(window.dateFormat),
        end: new Date($scope.selectedSprint.details.end).dateFormat(window.dateFormat)
      };
    }
    refreshChart();
  });


  function disableModes() {
    $scope.editMode = false;
    $scope.removeDecisionMode = false;
  }

  $scope.$on("boardStateChanged", function (event, sprintId) {
    if (sprintId == $scope.selectedSprint.id) {
      refreshChart();
    }
  });

  $scope.finishSprint = function () {
    wrapServiceCall(function() {
      return scrumSimulatorSvc.finishSprint($scope.selectedSprint.id);
    });
  };

  $scope.removeSprint = function () {
    wrapServiceCall(function() {
      return scrumSimulatorSvc.removeSprint($scope.selectedSprint.id);
    });
  };

  $scope.editSprint = function () {
    $scope.selectedSprint = null;
    var start = new Date();
    var end = new Date(start);
    end.setDate(start.getDate() + 7); //FIXME: konfigurowalne
    $scope.editedSprint = {
      name: "Sprint " + (nextSprintId() + 1), // +1 for natural indexing,
      start: start.dateFormat(window.dateFormat),
      end: end.dateFormat(window.dateFormat)
    };
    $scope.editMode = true;
  };

  function nextSprintId() {
    var maxId = -1;
    for (var i = 0; i < $scope.projectState.sprints.length; i++) {
      var sprint = $scope.projectState.sprints[i];
      var parsedId = parseInt(sprint.id);
      if (!isNaN(parsedId)) {
        maxId = Math.max(maxId, parsedId);
      }
    }
    return maxId + 1
  }

  $scope.askForRemoveSprint = function () {
    $scope.removeDecisionMode = true;
  };

  $scope.discard = function () {
    $scope.selectedSprint = $scope.sprintsOrdered[0];
    disableModes();
  };

  $scope.startSprint = function () {
    wrapServiceCall(function() {
      var input = {
        name: $scope.editedSprint.name,
        start: Date.parseDate($scope.editedSprint.start, window.dateFormat).toISOString().replace(/\..*Z/, "Z"),
        end: Date.parseDate($scope.editedSprint.end, window.dateFormat).toISOString().replace(/\..*Z/, "Z")
      };
      return scrumSimulatorSvc.startSprint(input);
    });
  };
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
          var d = new Date(x);
          return d.dateFormat(window.dateFormat);
        },
        yFormatter: function(y) {
          return y;
        }
      });

      scope.$watch("history", function(history) {
        while (series.length) {
          series.pop();
        }
        if (history) {
          startDate = history.startDate;
          for (var i = 0; i < history.series.length; i++) { // przepisujemy, bo wykres ma uchwyt do serii
            var column = history.series[i];
            if (column.name == 'Estimate') {
              column.color = "rgba(255, 0, 0, 0.5)";
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
        } else {
          startDate = null;
        }

        graph.render();
        legend.render();

        new Rickshaw.Graph.Behavior.Series.Toggle({ // to musi być wywyołane po przerenderowaniu legendy
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