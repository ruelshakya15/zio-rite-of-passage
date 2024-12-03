package com.rockthejvm.reviewboard.common

import scala.scalajs.js.annotation.* // for image
import scala.scalajs.js
import scala.scalajs.LinkingInfo
import org.scalajs.dom.window

object Constants {
  @js.native
  @JSImport("/static/img/fiery-lava 128x128.png", JSImport.Default)
  val logoImage: String = js.native // map scala string to js or vice versa

  @js.native
  @JSImport("/static/img/generic_company.png", JSImport.Default)
  val companyLogoPlaceholder: String = js.native // map scala string to js or vice versa

  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""

  val urlRegex       = """^(https?):\/\/(([^:/?#]+)(?::(\d+))?)(\/[^?#]*)?(\?[^#]*)?(#.*)?"""

    // in production -> companies.rockthejvm.com/...
      // companies.rockthejvm.com/api/...
  val backendBaseUrl = 
    if (LinkingInfo.developmentMode) "http://localhost:4041"
    else window.location.origin

}
