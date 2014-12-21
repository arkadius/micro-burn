angular.module('LiftChat', ['ChatServices'])

.controller('ChatController', ['$scope', '$timeout', 'chatSvc', function($scope, $timeout, chatSvc){
  $scope.onSend = function() {
    chatSvc.sendChat($scope.chatInput);
    $scope.chatInput = '';
  };

  $scope.newUsers = [];

  $scope.chat = {
    msgs : []
  };

  $scope.$on('newUser', function(event, data){
    $scope.newUsers.push(data);
    $timeout(function(){
      $scope.newUsers.shift();
    }, 5000);
  });
}])

;