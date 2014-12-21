$.getJSON( "history?sprintId=120", function(response) {
    console.log(response);

    var graph = new Rickshaw.Graph( {
        element: document.querySelector("#chart"),
        renderer: "line",
        interpolation: "step-after",
//        width: 580,
//        height: 250,
        series: response.series
    } );


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