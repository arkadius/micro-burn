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

app.controller("ProjectCtrl", ['$scope', '$timeout', 'historySvc', 'scrumSimulatorSvc', function ($scope, $timeout, historySvc, scrumSimulatorSvc) {
  window.scrumSimulatorSvc = scrumSimulatorSvc;

  $scope.projectState = {
    sprints: []
  };
  $scope.sprintsOrdered = [];
  $scope.existsActiveSprint = false;

  $scope.editedSprint = {
    name: "",
    start: "",
    end: "",
    baseStoryPoints: ""
  };

  $scope.editMode = false;
  $scope.removeDecisionMode = false;
  $scope.editBaseMode = false;

  $scope.$watch("projectState", function (projectState) {
    if (projectState.sprints.length > 0) {
      var existsActiveSprint = false;
      projectState.sprints.forEach(function (sprint) {
        if (sprint.details.isActive) {
          sprint.details.formattedName = sprint.details.name;
          sprint.order = "1" + sprint.details.start; // active on top of list
          existsActiveSprint = true;
        } else {
          sprint.details.formattedName = sprint.details.name + " (inactive)";
          sprint.order = "0" + sprint.details.start;
        }
      });
      $scope.existsActiveSprint = existsActiveSprint;
      projectState.sprints.sort(function (f, s) {
        return s.order.localeCompare(f.order);
      });
      $scope.sprintsOrdered = projectState.sprints;
      $scope.selectedSprint = $scope.sprintsOrdered[0];
    } else {
      $scope.existsActiveSprint = false;
      $scope.sprintsOrdered = [];
      $scope.selectedSprint = null;
      $scope.editedSprint = {
        name: "",
        start: "",
        end: "",
        baseStoryPoints: ""
      };
    }
    disableModes();
  });

  function disableModes() {
    $scope.editMode = false;
    $scope.removeDecisionMode = false;
    $scope.editBaseMode = false;
  }

  $scope.$watch("selectedSprint", function (sprint) {
    if (sprint) {
      $scope.editedSprint = {
        name: "",
        start: new Date($scope.selectedSprint.details.start).dateFormat(window.dateFormat),
        end: new Date($scope.selectedSprint.details.end).dateFormat(window.dateFormat),
        baseStoryPoints: $scope.selectedSprint.baseStoryPoints
      };
    }
    refreshChart();
  });

  $scope.$on("boardStateChanged", function (event, sprintId) {
    if (sprintId == $scope.selectedSprint.id) {
      refreshChart();
    }
  });

  function refreshChart() {
    if ($scope.selectedSprint) {
      historySvc.getHistory($scope.selectedSprint.id).then(function (history) {
        $scope.history = history;
      });
    } else {
      $scope.history = null;
    }
  }

  $scope.cancelBase = function () {
    $("#base-sp").blur();
    $scope.editBaseMode = false;
    $scope.editedSprint.baseStoryPoints = $scope.selectedSprint.baseStoryPoints;
  };

  $scope.saveBase = function () {
    $("#base-sp").blur();
    wrapServiceCall(function() {
      var input = {
        id: $scope.selectedSprint.id,
        baseStoryPoints: parseFloat($scope.editedSprint.baseStoryPoints)
      };
      return scrumSimulatorSvc.defineBase(input);
    });
  };

  $scope.editSprint = function () {
    $scope.selectedSprint = null;
    var start = new Date();
    var end = new Date(start);
    end.setDate(start.getDate() + 7); //TODO: konfigurowalne
    $scope.editedSprint = {
      name: "Sprint " + nextSprintNo(),
      start: start.dateFormat(window.dateFormat),
      end: end.dateFormat(window.dateFormat),
      baseStoryPoints: ""
    };
    $scope.editMode = true;
    $timeout(function (){
      $scope.$broadcast('editModeEntered');
    });
  };

  function nextSprintNo() {
    var maxId = 0;
    $scope.projectState.sprints.forEach(function(sprint) {
      var parsedId = parseInt(sprint.details.name.replace( /^\D+/g, ''));
      if (!isNaN(parsedId)) {
        maxId = Math.max(maxId, parsedId);
      }
    });
    return maxId + 1
  }

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

  $scope.finishSprint = function () {
    wrapServiceCall(function() {
      return scrumSimulatorSvc.finishSprint($scope.selectedSprint.id);
    });
  };

  $scope.askForRemoveSprint = function () {
    $scope.removeDecisionMode = true;
  };

  $scope.removeSprint = function () {
    wrapServiceCall(function() {
      return scrumSimulatorSvc.removeSprint($scope.selectedSprint.id);
    });
  };

  $scope.discardEdit = function () {
    $scope.selectedSprint = $scope.sprintsOrdered[0];
    disableModes();
  };

  $scope.discardRemove = function () {
    disableModes();
  }
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
      window.graph = graph;

      function toDays(millis) {
        return Math.floor((millis - startDate) / (1000 * 60 * 60 * 24))
      }

      function ticksBetween(minX, maxX) {
        var maxTicks = 7;
        var delta = (maxX - minX) / maxTicks;
        return _.range(0, maxTicks+1).map(function (i) {
          return minX + i * delta;
        });
      }

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

          var minX = Number.MAX_VALUE;
          var maxX = Number.MIN_VALUE;
          history.series.forEach(function (column, i) { // przepisujemy, bo wykres ma uchwyt do serii
            if (column.name == 'Estimate') {
              column.color = "rgba(255, 0, 0, 0.5)";
            } else if (i == history.series.length - 1) { // FIXME: ostatni DONE powinien być rozpoznawany za pomocą isDone
              column.color = "rgba(0, 0, 0, 0.9)";
            } else {
              column.color = "rgba(0, 0, 255, 0." + i + ")";
            }
            if ($cookies["disabled_" + i]) {
              column.disabled = true;
            }
            column.data.forEach(function (probe) {
              minX = Math.min(minX, probe.x);
              maxX = Math.max(maxX, probe.x);
            });
            series.push(column);
          });
          xAxes.tickValues = ticksBetween(minX, maxX);
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

app.directive('focusOn', function() {
  return function(scope, elem, attr) {
    scope.$on(attr.focusOn, function(e) {
      elem[0].focus();
    });
  };
});

app.directive('selectOnClick', function () {
  return {
    restrict: 'A',
    link: function (scope, element, attrs) {
      element.on('click', function () {
        this.select();
      });
    }
  };
});