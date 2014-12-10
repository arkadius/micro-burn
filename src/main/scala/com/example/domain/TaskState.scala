package com.example.domain

sealed trait TaskState

case object Completed extends TaskState
case object Opened extends TaskState
