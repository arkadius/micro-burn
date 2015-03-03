window.dateFormat = 'Y-m-d H:i';

function wrapServiceCall(call) {
  $("#cover").show();
  var callResult = call();
  callResult.catch(function (msg) {
    window.alert("Error: " + msg);
    $("#cover").hide();
  });
  callResult.then(function () {
    $("#cover").hide();
  });
}

var app = angular.module("MicroBurnApp", ["MicroBurnServices", 'ngCookies']);

var ctrlDeclaration = ['$scope', '$timeout', 'historySvc'];

if (window.sprintsManagement) {
  ctrlDeclaration.push('scrumSimulatorSvc');
}

ctrlDeclaration.push(function ($scope, $timeout, historySvc, scrumSimulatorSvc) {
  $scope.baseManagement = window.baseManagement;
  $scope.sprintsManagement = window.sprintsManagement;

  var clientFetchPromise = null;

  $scope.projectState = {
    sprints: []
  };
  $scope.sprintsOrdered = [];
  $scope.existsActiveSprint = false;
  $scope.selectedSprintMissingOrActive = false;

  $scope.editedSprint = {
    name: "",
    start: "",
    end: "",
    baseStoryPoints: ""
  };

  $scope.createMode = false;
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
    $scope.createMode = false;
    $scope.removeDecisionMode = false;
    $scope.cancelEditStart();
    $scope.cancelEditEnd();
    $scope.cancelEditBase();
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
    $timeout.cancel(clientFetchPromise);
    if ($scope.selectedSprint) {
      wrapServiceCall(function () {
        return historySvc.getHistory({sprintId: $scope.selectedSprint.id}).then(function (history) {
          $scope.history = history;
        }).finally(function () {
          clientFetchPromise = $timeout(refreshChart, window.clientFetchIfNoChangesPeriod);
        });
      });
    } else {
      $scope.history = null;
      clientFetchPromise = $timeout(refreshChart, window.clientFetchIfNoChangesPeriod);
    }
  }

  $scope.enterEditStartMode = function () {
    $scope.editStartMode = true;
    $scope.cancelEditEnd();
    $scope.cancelEditBase();
  };
  $scope.enterEditEndMode = function () {
    $scope.cancelEditStart();
    $scope.editEndMode = true;
    $scope.cancelEditBase();
  };
  $scope.enterEditBaseMode = function () {
    $scope.cancelEditStart();
    $scope.cancelEditEnd();
    $scope.editBaseMode = true;
  };

  $scope.cancelEditStart = function () {
    $("#start-date").blur();
    $(".xdsoft_datetimepicker").hide();
    $scope.editStartMode = false;
    if ($scope.selectedSprint)
      $scope.editedSprint.start = new Date($scope.selectedSprint.details.start).dateFormat(window.dateFormat);
  };
  $scope.cancelEditEnd = function () {
    $("#end-date").blur();
    $(".xdsoft_datetimepicker").hide();
    $scope.editEndMode = false;
    if ($scope.selectedSprint)
      $scope.editedSprint.end = new Date($scope.selectedSprint.details.end).dateFormat(window.dateFormat);
  };
  $scope.cancelEditBase = function () {
    $("#base-sp").blur();
    $scope.editBaseMode = false;
    if ($scope.selectedSprint)
      $scope.editedSprint.baseStoryPoints = $scope.selectedSprint.baseStoryPoints;
  };

  $scope.saveStart = function () {
    wrapServiceCall(function() {
      var input = {
        id: $scope.selectedSprint.id,
        startDate: Date.parseDate($scope.editedSprint.start, window.dateFormat).toISOString().replace(/\..*Z/, "Z")
      };
      return scrumSimulatorSvc.updateStartDate(input);
    });
  };
  $scope.saveEnd = function () {
    wrapServiceCall(function() {
      var input = {
        id: $scope.selectedSprint.id,
        endDate: Date.parseDate($scope.editedSprint.end, window.dateFormat).toISOString().replace(/\..*Z/, "Z")
      };
      return scrumSimulatorSvc.updateEndDate(input);
    });
  };
  $scope.saveBase = function () {
    wrapServiceCall(function() {
      var input = {
        id: $scope.selectedSprint.id,
        baseStoryPoints: parseFloat($scope.editedSprint.baseStoryPoints)
      };
      return scrumSimulatorSvc.defineBase(input);
    });
  };

  $scope.createSprint = function () {
    $scope.selectedSprint = null;
    var start = trimToHours(new Date());
    var end = plusDefaultSprintDuration(start);
    $scope.editedSprint.name = "Sprint " + nextSprintNo();
    $scope.editedSprint.start = start.dateFormat(window.dateFormat);
    $scope.editedSprint.baseStoryPoints = "";
    $scope.createMode = true;
    $timeout(function (){
      $scope.$broadcast('createModeEntered');
    });
  };

  $scope.$watch("editedSprint.start", function (start) {
    if ($scope.createMode) {
      var startDate = Date.parseDate($scope.editedSprint.start, window.dateFormat);
      if (startDate) {
        var end = plusDefaultSprintDuration(startDate);
        $scope.editedSprint.end = end.dateFormat(window.dateFormat);
      }
    }
  });

  function plusDefaultSprintDuration(start) {
    var end = new Date(start);
    end.setDate(start.getDate() + window.defaultSprintDuration);
    return end;
  }

  function trimToHours(date) {
    date.setMilliseconds(0);
    date.setSeconds(0);
    date.setMinutes(0);
    return date;
  }

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
    $scope.cancelEditStart();
    $scope.cancelEditEnd();
    $scope.cancelEditBase();
    wrapServiceCall(function() {
      return scrumSimulatorSvc.finishSprint({id: $scope.selectedSprint.id});
    });
  };

  $scope.askForRemoveSprint = function () {
    $scope.removeDecisionMode = true;
  };

  $scope.removeSprint = function () {
    wrapServiceCall(function() {
      return scrumSimulatorSvc.removeSprint({id: $scope.selectedSprint.id});
    });
  };

  $scope.discardCreate = function () {
    $scope.selectedSprint = $scope.sprintsOrdered[0];
    $scope.editedSprint.start = ""; // nie można przenieść do obsługi selectedSprint bo zepsuje się createSprint
    $scope.editedSprint.end = "";
    disableModes();
  };

  $scope.discardRemove = function () {
    disableModes();
  }
});

app.controller("ProjectCtrl", ctrlDeclaration);

app.directive('sprintChart', ['$cookies', function ($cookies) {
  return {
    template: "<div id='y-axis'></div>" +
              "<div id='chart'></div>" +
              "<div id='legend'></div>",
    link: function (scope, element, attrs) {
      var series = [];
      var startDate = null;

      element.attr("id", "sprint-chart-container");

      graph = new Rickshaw.Graph({
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
        return Math.round((millis - startDate) / (1000 * 60 * 60 * 24))
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

      var lineAnnotate = new LineAnnotate({
        graph: graph
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
            } else if (column.doneColumn) {
              column.color = "rgba(0, 0, 0, 0.9)";
            } else {
              column.color = "rgba(0, 0, 255, 0." + i + ")";
            }
            var storedEnabled = $cookies["enabled_" + i];
            if (storedEnabled == "0" || !storedEnabled && i == 1) { // pierwsza kolumna bez zmiany scope'a będzie pokazywała 0
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
          xAxes.tickValues = null;
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
              $cookies["enabled_" + index] = '0';
            });
          };
          s.enable = function () {
            s.disabled = false;
            scope.$apply(function () {
              $cookies["enabled_" + index] = '1';
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