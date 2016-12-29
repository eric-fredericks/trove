/*
 *  # BudgetFree
 *
 *  This file is part of BudgetFree - A FREE desktop budgeting application that
 *  helps you track your finances and literally FREES you from complex budgeting.
 *
 *  Copyright © 2016-2017 Eric John Fredericks.
 *
 *  BudgetFree is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  BudgetFree is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with BudgetFree.  If not, see <http://www.gnu.org/licenses/>.
 */

package budgetfree.ui

import budgetfree.constants.{ApplicationName, ApplicationVersion}
import budgetfree.ui.ButtonTypes._

import scala.io.Source
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, _}
import scalafx.scene.layout.VBox

private[ui] class HelpAboutDialog extends Alert(AlertType.Information) {
  title = ApplicationName
  initOwner(Main.stage)
  graphic = ApplicationIconImageView
  headerText = "About BudgetFree"

  val appLabel_1 = Label(s"BudgetFree Version $ApplicationVersion")
  val description_1 = Label("A FREE desktop application that helps you track your finances")
  val description_2 = Label("and literally FREES you from complex budgeting!")
  val copyrightLabel = Label("Copyright © 2016-2017 Eric John Fredericks")
  val licenseLinkLabel = Label("This software is licensed  under the")
  val licenseLink = new Hyperlink {
    text = "GNU General Public License, version 3.0"
    onAction = (_: ActionEvent) => { Main.hostServices.showDocument("https://www.gnu.org/licenses/gpl-3.0.txt")}
  }
  val iconLinkLabel = Label("Icons provided are free for personal or commercial use under license by")
  val iconLink = new Hyperlink {
    text = "Icons8."
    onAction = (_: ActionEvent) => { Main.hostServices.showDocument("https://icons8.com")}
  }
  val thirdPartyLicenseLinkLabel = Label("This software incorporates many open source libraries.")

  private[this] def thirdPartyLicenseText = {
    val fileContents = Source.fromFile(ThirdPartyLicenseUrlTextUri).getLines.mkString("\n")
    val ta = new TextArea(fileContents)
    ta.editable = false
    ta
  }

  val thirdPartyLicenseButton = new Button {
    mnemonicParsing = true
    text = "_Third-Party Licenses..."

    tooltip = "Click here for third-party licensing information"
    onAction = _ => new Alert(AlertType.Information) {
      title = ApplicationName
      initOwner(Main.stage)
      headerText = "Third Party Licensing"
      dialogPane().content = thirdPartyLicenseText
      dialogPane().setPrefSize(700, 800) // I tried setting the width/height values and the width didn't work.
      buttonTypes = Seq(Ok)
      resizable = false
    }.showAndWait()
  }

  val theContent = new VBox {
    children = Seq(appLabel_1,
      blankLabel,
      description_1, description_2,
      blankLabel,
      copyrightLabel,
      blankLabel,
      licenseLinkLabel, licenseLink,
      blankLabel,
      iconLinkLabel, iconLink,
      blankLabel,
      thirdPartyLicenseLinkLabel,
      blankLabel,
      thirdPartyLicenseButton
    )
  }

  dialogPane().content = theContent
  buttonTypes = Seq(Ok)
  // Linux workaround
  resizable = true
  //dialogPane().setPrefSize(600, 400) // I tried setting the width/height values and the width didn't work.
  resizable = false

}