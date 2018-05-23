package com.innovyria

import java.util.concurrent
import javafx.embed.swing.JFXPanel

import akka.actor.{Actor, ActorSystem, Props}
import scribe.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scalafx.application.Platform
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.layout.BorderPane
import scalafx.stage.Stage

sealed trait Message

sealed trait Command extends Message

sealed trait Response extends Message

case object Gui {

  case object Start extends Command

  case object Stop extends Command

}

case object Ping extends Command

case object Pong extends Response

case object App extends Logging {
  def main(args: Array[String]): Unit = {
    Platform.implicitExit = false
    logger.info(s"Starting $productPrefix")
    new JFXPanel()
    ActorSystem("App").actorOf(Props[AppFsm], "fsm")
  }
}

case class AppFsm() extends Actor with Logging {

  context.system.scheduler.schedule(1 seconds, 1 seconds, self, Ping)

  override def preStart(): Unit = {
    super.preStart()
    self ! Gui.Start
  }

  override def receive: Receive = {
    case Gui.Start =>
      logger.info("Received Gui.Start")
      showInDialog("App")
    case Gui.Stop =>
      logger.info("Received Gui.Stop")
      Platform.exit()
      context.system.terminate()
    case Ping =>
      logger.info("Received Ping")
      self ! Pong
    case Pong =>
      logger.info("Received Pong")
  }

  private def showInDialog(message: String) {
    val callable = new concurrent.Callable[Unit] {
      override def call(): Unit = new Stage {
        outer =>
        title = "App Dialog"
        scene = new Scene {
          root = new BorderPane {
            padding = Insets(25)
            bottom = new Button {
              text = message
              onAction = scalafx.Includes.handle {
                outer.close()
                self ! Gui.Stop
              }
            }
          }
        }
      }.show()
    }
    Platform.runLater(new concurrent.FutureTask(callable))
  }
}