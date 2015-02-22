Settings
========

Settings are written in [Typesafe config format](https://github.com/typesafehub/config). In short it is a human-friendly JSON superset.

# Project settings

## project.dataRoot
Place where will be kept all data of application. Default: *"data"*

## project.splitSpBetweenTechnicalTasks
If will be turned on, story points will be splitted between technical tasks which hans't got defined story points. Default: *false*

## project.dayOfWeekWeights
Weights for days for estimates, e.g. *\[1, 1, 1, 1, 1, 5, 3\]*. You can define your own or use predefined:
*${predefined.businessWeek}*, *${predefined.fullTime}*, *${predefined.afterWork}*. Default: *${predefined.businessWeek}*

## project.management
For integrations that hasn't got own sprints management, you can define management settings

### project.management.mode
Management mode. Available options:
- *"manual"* - you must start/finish sprint manually
- *"auto"* - sprint is finished/started periodically
Default: *"manual"*

### project.management.period
Period for automatic management mode. Available options:
- *"every-n-days"* - sprints are finished/started every n days
- *"every-n-weeks"* - sprints are finished/started every n weeks; you can define day-of-week or monday will be used
- *"every-n-months"* - sprints are finished/started every n months; you can define day-of-month or 1st will be used
Default: *undefined*

### project.management.n
Depending on period type: count of days/weeks/months. Default: *1*

### project.management.time
Time when sprint will be finished/started automatically. Default: "00:00"

### project.management.start-date
Date from when sprints will be automatically finished/started. Default: *undefined*

## project.sprint-base
Strategy how base story points count will be calculated. Available options:
- *"auto-on-sprint-start"* - base will be automatically calculated on sprint start based on how many story points is not done
- *"auto-on-scope-change"* - base will be automatically re-calculated on sprint scope change, tasks finished on sprint start and not reopened after that will be skipped
- *"specified(number)"* - base will be constant
Default: "auto-on-sprint-start" for manual management mode and "auto-on-scope-change" for automatic management mode

# Connector settings

## connector.port
Port on which application will listen on. Default *8080*

## connector.contextPath
Context path from which application will available. Default *"/"*

# Authorization

## authorization.secretForScrumSimulation
Secret string which must be passed as a url parameter e.g. *http://localhost:8080/?secret=some-secret-password* for sprints management. Default: *undefined* - no authorization

# Defaults

All defaults are available at [defaults.conf](https://github.com/arkadius/micro-burn/blob/master/src/main/resources/defaults.conf)

# Predefined

Predefined values are available at [predefined.conf](https://github.com/arkadius/micro-burn/blob/master/src/main/resources/predefined.conf)