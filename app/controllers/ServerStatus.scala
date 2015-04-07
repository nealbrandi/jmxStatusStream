package controllers

import java.lang.management.ManagementFactory

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.{ Enumerator, Iteratee }
import play.api.mvc.{ Action, Controller, WebSocket }
import play.api.Logger

object ServerStatus extends Controller {

  def statusPage() = Action { implicit request =>
    Ok(views.html.serverStatus.statusPage(request))
  }

  /*
   * WebSocket Action
   *   The WebSocket.using method creates a WebSocket action instead of a
   *   regular HTTP action. Its type parameter, String, indicates that each 
   *   message that will be sent and received over this WebSocket connection 
   *   is a String.
   */
  def statusStream() = WebSocket.using[String] { implicit request =>

    Logger.info(s"Building socket stream handlers . . . ")

    // Utility method that acquires and formats system load as a string
    def getLoadAverage = {
      "%1.2f" format ManagementFactory.getOperatingSystemMXBean.getSystemLoadAverage()
    }

    /* Construct And Wire Stream Processing Components 
     *
     *   1. Given our design, there will be no incoming client mesaages. We  
     *      construct an Iteratee that ignores all input stream messages.
     *   2. To create a continious output stream we construct an Enumerator 
     *      that returns the system load as reported by the JMX bean every 3 
     *      seconds.
     */
    val in  = Iteratee.ignore[String]
    
    val out = Enumerator.repeatM {
      Promise.timeout(getLoadAverage, 3 seconds)
    }

    // We return a tuple containing the input Iteratee and output Enumerator.  These
    // components will now process the WebSocket's streams.
    (in, out)
  }
} 