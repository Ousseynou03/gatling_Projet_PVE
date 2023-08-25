package PVE

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure.ChainBuilder

import java.util.UUID.randomUUID

object Recherche_Produit_UGA {

  private val FichierPath: String = System.getProperty("dataDir", "data/")
  private val FichierJddPVE: String = "jddPVE.csv"

  private val FichierJddUSER: String = "jddUser.csv"

  private val FichierJddUGA: String = "jddUGA.csv"

  val jddDataPVE = csv(FichierPath + FichierJddPVE).circular
  val jddDataUser = csv(FichierPath + FichierJddUSER).circular
  val jddDataUGA = csv(FichierPath + FichierJddUGA).circular


  //Aauthentification du vendeur (obtention de l’access token)
  def Autentication() = {
    exec(http("POST")
      .post("https://ssotest.interne.galerieslafayette.com/auth/realms/GL-ENTREPRISE/protocol/openid-connect/token")
      .header("Content-Type", "application/x-www-form-urlencoded")
      .formParam("client_id", "gl-mobile-sdk")
      .formParam("client_secret", "#{CLIENT_SECRET}")
      .formParam("grant_type", "password")
      .formParam("username", "p_ppve")
      .formParam("password", "Service12345!")
      .check(jsonPath("$.access_token").saveAs("access_token")))
      .exec { session =>
        println("Token :" + session("access_token").as[String])
        session
      }
  }


  //Récupération informations venceur

  def getInfosVendeur() = {
    exec(http("Get_infos_vendeur")
      .get("auth/login/employee")
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("$.badgeNumber").saveAs("user_BadgeNumber"))
      .check(jsonPath("$.storeCode").saveAs("user_StoreCode"))
      .check(status.is(200)))
      .exec { session =>
        println("user_BadgeNumber : "+ session("user_BadgeNumber").as[String])
        println("user_StoreCode :" + session("user_StoreCode").as[String])
        session
      }
  }


  //Récupération des infos magasin vendeur
  def getInfosMagasin() = {
    exec(http("Get_infos_Magasin")
      .get("sap/stores/storeCode/#{user_StoreCode}")
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("$.sourceLatitude").saveAs("storeLatitude"))
      .check(jsonPath("$.sourceLongitude").saveAs("storeLongitude"))
      .check(status.is(200)))
  }

  ///////////////////////////////////////////////
  // Recherche de produit par UGA
  /////////////////////////////////////////////

  def rechercheProduitUga() = {
    exec(http("Recherche de produit par UGA")
      .get("product-orchestration/#{UGA}?type=uga&getCcmProduct=true")
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("$.sapProduct.price.value").saveAs("Price"))
      .check(jsonPath("$.sapProduct.categories[*].name").findAll.saveAs("PVE_IDENTIFIER"))
      .check(status.is(200)))
      .exec { session =>
        println("PVE_IDENTIFIER :" + session("PVE_IDENTIFIER").as[String])
        session
      }
  }

  //Récupération des informations concernant la recherche de produit par UGA
  def get_Infos_UGA() = {
    exec(http("Get Feature Flipping")
      .get("feature-flipping")
      .header("Authorization", "Bearer #{access_token}")
    )
      .exec(http("Get Product Stock")
        .get("estock/productStock?productCode=#{UGA}")
        .header("Authorization", "Bearer #{access_token}")
      )
      .exec(http("Get Nearby Stores")
        .get("sap/stores?latitude=#{storeLatitude}&longitude=#{storeLongitude}&pageSize=1000&radius=10000000")
        .header("Authorization", "Bearer #{access_token}")
      )
  }


  //Recommandations
  //${PVE_IDENTIFIER} ???
  def perform_Recommendations() = {
    exec(session => {
      val UUID_V4 = randomUUID()
      session.set("UUID_V4", UUID_V4)
    })
      .exec { session =>
        println("UUID_V4 : " + session("UUID_V4").as[String])
        session
      }
      .exec(http("Get Tracking Recommendation ID")
        .get("recoT2sPageId?brand=GL&recoType=TRACKING&pageScope=PRODUCT")
        .header("Authorization", "Bearer ${access_token}")
        .check(jsonPath("$.idT2sPage").saveAs("idT2sPageTracking"))
      )
      .exec(http("Get Additional Recommendation ID")
        .get("recoT2sPageId?brand=GL&recoType=ADDITIONAL&pageScope=PRODUCT&pveIdentifier=#{PVE_IDENTIFIER}")
        .header("Authorization", "Bearer ${access_token}")
        .check(jsonPath("$.idT2sPage").saveAs("idT2sPageAdditional"))
      )
      .exec(http("Get Similar Recommendation ID")
        .get("recoT2sPageId?brand=GL&recoType=SIMILAR&pageScope=PRODUCT&pveIdentifier=#{PVE_IDENTIFIER}")
        .header("Authorization", "Bearer ${access_token}")
        .check(jsonPath("$.idT2sPage").saveAs("idT2sPageSimilar"))
      )

      .exec(http("Recommendation Tracking")
        .post("recommendation/tracking")
        .header("Authorization", "Bearer ${access_token}")
        .body(StringBody(
          """
            |{
            |"pID":"#{idT2sPageTracking}",
            |"tID":"#{UUID_V4}",
            |"iID":["#{PVE_IDENTIFIER}"],
            |"eN":"VIEW"
            |}
            |""".stripMargin)).asJson
      )
      .exec(http("Recommendation for Similar")
        .post("recommendation")
        .header("Authorization", "Bearer ${access_token}")
        .body(StringBody(
          """
            |{
            |"tID":"#{UUID_V4}",
            |"iID":"#{UGA}-${COLOR_VARIANT}",
            |"pID":"#{idT2sPageSimilar}",
            |"setID":"#{user_StoreCode}"
            |}
            |""".stripMargin)).asJson
      )
      .exec(http("Recommendation for Additional")
        .post("recommendation")
        .header("Authorization", "Bearer ${access_token}")
        .body(StringBody(
          """
            |{
            |"tID":"#{UUID_V4}",
            |"iID":"#{UGA}-${COLOR_VARIANT}",
            |"pID":"#{idT2sPageAdditional}",
            |"setID":"#{user_StoreCode}"
            |}
            |""".stripMargin)).asJson
      )
  }

  val scnRechercheParUGA = scenario("TEST PERF PROJET PVE")
    .feed(jddDataPVE)
    .feed(jddDataUGA)
    .exec(Autentication())
    .exec(getInfosVendeur())
    .exec(getInfosMagasin())
    .exec(rechercheProduitUga())
    .exec(get_Infos_UGA())
    .exec(perform_Recommendations())
    .exec(perform_Recommendations())


}
