micro-burn
==========

# Overview

micro-burn is simple microservice providing burndownchart. At this moment it has **Jira Agile** integration. If you want to contribute and add new provider, take a look at [Example provider implementation](https://github.com/arkadius/micro-burn/tree/master/src/main/scala/org/github/microburn/integration/jira) and send me a PR.

Q: Why to write another burdownchart if Jira Agile already have it?<br>
A: Because Jira Agile's burdownchart has some drawbacks:
* it doesn't report points for completion of subtasks
* it treats new added to sprint tasks as a sprint regress
* it shows only last column changes

# Run

* Download the [latest](https://github.com/arkadius/micro-burn/releases/latest) release
* create *application.conf* configuration file, example:
```conf
board.columns = [
  {
    name: "To Do"
    statusIds: [1, 4]
  }
  {
    name: "In Progress"
    statusIds: [3]
  }
  {
    name: "To review"
    statusIds: [10067]
  }
  {
    name: "Ready to test"
    statusIds: [10064]
  }
  {
    name: "Tested",
    statusIds: [10065, 10045, 10048, 10010, 5]
  }
]

jira {
  url = "https://localhost:8088/jira/rest/api/latest"
  user = "test"
  password = "test"
  greenhopper {
    url = "https://localhost:8088/jira/rest/greenhopper/1.0"
    storyPointsField = "customfield_10192"
    rapidViewId = 56
  }
}
```
* run `java -jar micro-burn-${version}.jar`
* open in browser: http://localhost:8080

# Configuration Q&A

Q: How to configure board columns?<br>
A: The simplest way is to copy-paste them from result of *${greenhopper.url}/xboard/work/allData.json?rapidViewId=${greenhopper.rapidViewId}* (is available at path: */columnsData/columns*)

Q: Hot to get *rapidViewId*?<br>
A: Is available in board's url

Q: Hot to get *storyPointsField*?<br>
A: From the same place where board columns – (at path */issuesData/issues[1]/estimateStatistic/statFieldId*)

Q: How to change port?<br>
A: Override the value of *jetty.port*

Q: Where are other settings?<br>
A: All defaults are available at [defaults.conf](https://github.com/arkadius/micro-burn/blob/master/src/main/resources/defaults.conf)

# Implementation notice

As a web framework was used great scala framework [lift](https://github.com/lift/framework) with best Comet support.<br>
For a bridge with Angular was used [lift-ng](https://github.com/joescii/lift-ng).<br>
Charts where drawed in [rickshaw](https://github.com/shutterstock/rickshaw).<br>

micro-burn is a microservice. It does mean, that it integrates with 3rd-part tools by REST API. Application uses event souring – in the date folder there are kept: initially fetched board state, last (snapshot state) and task events in CSV format. It does mean, that application will show only changes which were collected during its run. All archive changes will be unavailable.

# License

The micro-burn is released under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).

# Screenshot

![screenshot](https://raw.githubusercontent.com/arkadius/micro-burn/screenshots/micro-burn.png)
