package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.*
import org.scalajs.dom.Element
import org.scalajs.dom.File
import org.scalajs.dom.FileReader

import zio.*

import com.rockthejvm.reviewboard.common.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.http.requests.*

case class CreateCompanyState(
    name: String = "",
    url: String = "",
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = List(),
    upStreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  def errorList: List[Option[String]] =
    List(
      Option.when(name.isEmpty())("The name cannot be empty"),
      Option.when(!url.matches(Constants.urlRegex))("The URL is invalid")
    ) ++ upStreamStatus.map(_.left.toOption).toList
  def maybeSuccess: Option[String] =
    upStreamStatus.flatMap(_.toOption)

  def toRequest: CreateCompanyRequest =
    CreateCompanyRequest(
      name,
      url,
      location,
      country,
      industry,
      image,
      Option(tags).filter(_.nonEmpty)
    )
}

object CreateCompanyPage extends FormPage[CreateCompanyState]("Post New Company") {
  override def basicState: CreateCompanyState = CreateCompanyState()
  override def renderChildren() =
    List(
      renderInput("Company Name", "name", "text", true, "ACME Inc", (s, v) => s.copy(name = v)),
      renderInput(
        "Company URL",
        "url",
        "text",
        true,
        "https://acme.com",
        (s, v) => s.copy(url = v)
      ),
      renderLogoUpload("Company logo", "logo"),
      img(
        src <-- stateVar.signal.map(_.image.getOrElse(""))
      ),
      renderInput(
        "Location",
        "location",
        "text",
        false,
        "Somewhere",
        (s, v) => s.copy(location = Some(v))
      ),
      renderInput(
        "Country",
        "url",
        "text",
        false,
        "Some country",
        (s, v) => s.copy(country = Some(v))
      ),
      renderInput(
        "Industry",
        "industry",
        "text",
        false,
        "Funtional code",
        (s, v) => s.copy(industry = Some(v))
      ),
      renderInput(
        "Tags - seperate by ','",
        "tags",
        "text",
        false,
        "Scala, zio",
        (s, v) => s.copy(tags = v.split(",").map(_.trim).toList)
      ),
      button(
        `type` := "button",
        "Post Company",
        onClick.preventDefault.mapTo(stateVar.now()) --> submitter
      )
    )

  private def renderLogoUpload(name: String, uid: String, isRequired: Boolean = false) = {
     div(
      cls := "row",
      div(
        cls := "col-md-12",
        div(
          cls := "form-input",
          label(
            forId := uid,
            cls := "form-label",
            if (isRequired) span("*") else span(),
            name
          ),
          div (
            cls := "image-upload",
            input(
              `type` := "file",
              cls := "form-control",
              idAttr := uid,
              accept := "image/*",
              // NOTE: fileReader can read files in asynchronous manner using callbacks
              // .mapToFiles gives a List[Files]
              onChange.mapToFiles --> fileUploader
            ),
            img (
              cls := "image-upload-thumbnail",
              src <-- stateVar.signal.map(_.image.getOrElse(Constants.companyLogoPlaceholder))
            )
          )
        )
      )
    )
  }

  private def computeDimensions(width: Int, height: Int): (Int, Int) =
    if (width >= height) {
      val ratio = width * 1.0 / 256 // 1.0 to use doubles
      val newW  = width / ratio
      val newH  = height / ratio
      (newW.toInt, newH.toInt)
    } else {
      val (newH, newW) =
        computeDimensions(
          height,
          width
        ) // recursive call exchanging height and width (if width is smaller)
      (newW, newH)
    }

  // File,FileReader from "scalajs.dom"
  val fileUploader = (files: List[File]) => {
    // take the file
    val maybeFile = files.headOption.filter(_.size > 0)
    maybeFile.foreach { file =>
      // make a FileReader
      val reader = new FileReader
      // register onload callback => when it's done, populate my state with reader.result
      reader.onload = _ => {
        // 256x256
        // draw the picture into a canvas 256x256
        // make a fake img tag (not rendered) - img2
        val fakeImage = document.createElement("img").asInstanceOf[org.scalajs.dom.HTMLImageElement]
        fakeImage.addEventListener(
          "load",
          _ => {
            val canvas          = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
            val context         = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
            val (width, height) = computeDimensions(fakeImage.width, fakeImage.height)
            canvas.width = width
            canvas.height = height
            // render the original image into that canvas
            context.drawImage(fakeImage, 0, 0, width, height)
            // set the state to the text repr of img2
            stateVar.update(_.copy(image = Some(canvas.toDataURL(file.`type`))))
          }
        )
        fakeImage.src = reader.result.toString
      }
      // trigger the reader
      reader.readAsDataURL(file)
    }
  }

  val submitter = Observer[CreateCompanyState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(_.company.createEndpoint(state.toRequest))
        .map { userResponse =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upStreamStatus = Some(Right("Company posted! Check it out in the companies list."))
            )
          )
        }
        .tapError { e =>
          ZIO.succeed {
            stateVar.update(
              _.copy(showStatus = true, upStreamStatus = Some(Left(e.getMessage())))
            )
          }
        }
        .runJs
    }
  }

}
