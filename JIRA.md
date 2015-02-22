Jira Agile Integration
======================

Q: Why to write another burdownchart if Jira Agile already have it?<br>
A: Because Jira Agile's burdownchart has some drawbacks:
* it doesn't report points for completion of subtasks
* it treats new added to sprint tasks as a sprint regress
* it shows only last column changes

# Sample configuration

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

# Configuration Q&A

Q: How to configure board columns?<br>
A: The simplest way is to copy-paste them from result of *${greenhopper.url}/xboard/work/allData.json?rapidViewId=${greenhopper.rapidViewId}* (is available at path: */columnsData/columns*)

Q: Hot to get *rapidViewId*?<br>
A: It is available in board's url

Q: Hot to get *storyPointsField*?<br>
A: From the same place where board columns – (at path */issuesData/issues[1]/estimateStatistic/statFieldId*)