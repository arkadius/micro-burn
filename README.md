micro-burn
==========

[![Build Status](https://travis-ci.org/arkadius/micro-burn.svg?branch=master)](https://travis-ci.org/arkadius/micro-burn)

# Overview

micro-burn is a simple microservice providing burndownchart. At this moment it has **Trello** and **Jira Agile** integration. If you want to contribute and add new provider, take a look at [Example provider implementation](https://github.com/arkadius/micro-burn/tree/master/src/main/scala/org/github/microburn/integration/trello) and send me a PR.

# Run

* Download the [latest](https://github.com/arkadius/micro-burn/releases/latest) release
* create *application.conf* configuration file
* run `java -jar micro-burn-${version}.jar`
* open in browser: [http://localhost:8080](http://localhost:8080)

# Configuration

To run application you must provide at least integration service: trello/jira configuration and board columns. All other settings are optional.

## Trello configuration

By default, card without story points will have zero story points. If you want to define some, you can add them in curly braces before task name e.g. "(5) Task".
You can also use [Scrum for trello](http://scrumfortrello.com/) extension for your browser. Also you can specify *project.defaultStoryPointsForUserStories*.
Checklist items are treated as technical tasks, you can also specify story points for them, but also you can turn on splitSpBetweenTechnicalTasks and
story points from cards will be splitted between checklist items.

### Sample configuration

```conf
project {
  defaultStoryPointsForUserStories = 1
  splitSpBetweenTechnicalTasks = true
  dayOfWeekWeights = ${predefined.afterWork}
}

project.boardColumns = [
  {
    "id": "example backlog column id",
    "name": "Backlog",
    "backlogColumn": true
  },
  {
    "id": "example todo column id",
    "name": "TODO",
  },
  {
    "id":"example in progress column id",
    "name":"In Progress"
  },
  {
    "id": "example done column id",
    "name": "Done",
    "doneColumn": true
  }
]

trello {
  token = "example token"
  boardId = "example board id"
}
```

### Configuration Q&A

Q: How can I get token?<br>
A: You must [authorize micro-burn](https://trello.com/1/authorize?key=8b5ab0f2d93cb4717d15876fda44813c&name=micro-burn&expiration=never&response_type=token)

Q: How can I get boardId?<br>
A: From *https://trello.com/1/members/me?key=8b5ab0f2d93cb4717d15876fda44813c&token=${trello.token}*

Q: How can I get boardColumns<br>
A: From *https://trello.com/1/boards/${trello.boardId}/lists?key=8b5ab0f2d93cb4717d15876fda44813c&token=${trello.token}*

Q: What is it backlogColumn/doneColumn?<br>
A: You need to mark column as backlogColumn if you keep there long-term planned cards (those cards will be not shown in burndown). Done column will be drawn in black color on chart.

## Jira configuration

[CONFIGURATION.md](https://github.com/arkadius/micro-burn/blob/master/JIRA.md)

## Additional settings

[CONFIGURATION.md](https://github.com/arkadius/micro-burn/blob/master/SETTINGS.md)

# Implementation notice

As a web framework was used great scala framework [lift](https://github.com/lift/framework) with best Comet support.<br>
For a bridge with Angular was used [lift-ng](https://github.com/joescii/lift-ng).<br>
Charts where drawed in [rickshaw](https://github.com/shutterstock/rickshaw).<br>

micro-burn is a microservice. It integrates with 3rd-part tools by REST API. Application uses event souring – in the date folder there are kept: initially fetched board state, last (snapshot state) and task events in CSV format. Application will show only changes which were collected during its run. All other changes will be unavailable.

# License

The micro-burn is released under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).

# Screenshot

![screenshot](https://raw.githubusercontent.com/arkadius/micro-burn/screenshots/micro-burn.png)