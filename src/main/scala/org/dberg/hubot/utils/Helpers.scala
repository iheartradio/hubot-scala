package org.dberg.hubot.utils

import com.typesafe.config.ConfigFactory
import org.dberg.hubot.models.{ Message, MessageType }
import scala.util.matching.Regex
import scala.collection.JavaConversions._
import scalaj.http._
import org.json4s._
import org.json4s.jackson.JsonMethods._

object Helpers {

  case class HttpResponse(status: Int, headers: Map[String, IndexedSeq[String]], body: String)

  def regex(name: String): Regex = s"(?i)^[@]?$name".r
  def regexStr(name: String) = s"(?i)^[@]?$name\\s*"

  implicit class RobotMatcher(body: String) {

    def addressedToHubot(message: Message, hubotName: String): Boolean =
      message.messageType == MessageType.Direct || regex(hubotName).findFirstIn(body).isDefined

    def removeBotString(name: String): String =
      body.replaceFirst(regexStr(name), "")
  }

  implicit class StringImplicits(input: String) {
    def toJson = parse(input)
  }

  val config = ConfigFactory.load()

  def getConfString(key: String, default: String): String = config.hasPath(key) match {
    case false => default
    case true => config.getString(key)
  }

  def getConfStringList(key: String): Seq[String] = config.hasPath(key) match {
    case false => Seq()
    case true => config.getStringList(key)
  }

  implicit class HttpAuth(h: HttpRequest) {
    def withAuth(auth: Option[(String, String)]): HttpRequest = {
      if (auth.isDefined) {
        h.auth(auth.get._1, auth.get._2)
      } else h
    }
  }

  def request(url: String, method: String = "GET", headers: Seq[(String, String)], data: String = "", auth: Option[(String, String)] = None): HttpResponse = method.toUpperCase match {
    case "GET" =>
      val req = Http(url).withAuth(auth).headers(headers)
      HttpResponse(req.asString.code, req.asString.headers, req.asString.body)
    case "POST" =>
      val req = Http(url).withAuth(auth).headers(headers).postData(data)
      HttpResponse(req.asString.code, req.asString.headers, req.asString.body)
    case "PUT" =>
      val req = Http(url).withAuth(auth).headers(headers).put(data)
      HttpResponse(req.asString.code, req.asString.headers, req.asString.body)
    case "DELETE" =>
      val req = Http(url).withAuth(auth).headers(headers).postData(data).method("DELETE")
      HttpResponse(req.asString.code, req.asString.headers, req.asString.body)
  }

}
