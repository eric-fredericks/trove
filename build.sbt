/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2017 Eric John Fredericks.
 *
 *  Trove is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Trove is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Trove.  If not, see <http://www.gnu.org/licenses/>.
 */


organization := "net.ericfredericks"

name := "trove"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-deprecation", "-feature")

// UI
libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % "8.0.102-R11"
)

// Logging
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.22",
  "org.slf4j" % "slf4j-simple" % "1.7.22",
  "org.clapper" %% "grizzled-slf4j" % "1.3.0"
)

// Akka
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.16",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.16"
)

// ScalaTest and Scalactic
libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

// Database
//ejf-fixMe: refactor versions etc.
//ejf-fixMe: version check
//ejf-fixMe: add licenseing info
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.2.0-M2",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.0-M2",
  "org.xerial" % "sqlite-jdbc" % "3.16.1"
)

fork := true

