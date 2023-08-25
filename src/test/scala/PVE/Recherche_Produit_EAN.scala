package PVE

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure.ChainBuilder

import java.util.UUID.randomUUID

object Recherche_Produit_EAN {

  private val FichierPath: String = System.getProperty("dataDir", "data/")
  private val FichierJddPVE: String = "jddPVE.csv"
  private val FichierJddUGA: String = "jddUGA.csv"
  private val FichierJddEAN: String = "jddEAN.csv"

  val jddDataPVE = csv(FichierPath + FichierJddPVE).circular
  val jddDataEAN = csv(FichierPath + FichierJddEAN).circular
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


  /////////////////////////////////////////////////////
  ////// Recherche de produit par EAN /////
  ///////////////////////////////

  def get_Product_By_EAN() = {
    exec(http("Search Product by EAN")
      .get("{PVE_SERVER_URL}product-orchestration/#{EAN}?type=EAN&getCcmProduct=true")
      .header("Authorization", "Bearer #{access_token}")
      //.check(jsonPath("$.sapProduct.code").saveAs("UGA"))
      .check(jsonPath("$.sapProduct.price.value").saveAs("PRICE"))
      .check(jsonPath("$.sapProduct.categories[*].name").findAll.saveAs("PVE_IDENTIFIER"))
    )
  }


  def tracking_page() = {
    exec(http("Tracking Page")
      .get("recoT2sPageId?brand=GL&recoType=TRACKING&pageScope=PRODUCT")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("$.idT2sPage").saveAs("ID_T2S_PAGE_TRACKING"))
    )
  }

  def get_Additional_Recommandation()  = {
    exec(http("Get Additional Recommendation ID")
      .get("recoT2sPageId?brand=GL&recoType=ADDITIONAL&pageScope=PRODUCT&pveIdentifier=#{PVE_IDENTIFIER}")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("$.idT2sPage").saveAs("idT2sPageAdditional"))
    )
  }

  def get_Similar_Recommandation() = {
  exec(http("Get Similar Recommendation ID")
      .get("recoT2sPageId?brand=GL&recoType=SIMILAR&pageScope=PRODUCT&pveIdentifier=#{PVE_IDENTIFIER}")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("$.idT2sPage").saveAs("idT2sPageSimilar"))
    )
  }

  def get_Produit_Stock() = {
  exec(http("Get Nearby Stores")
      .get("sap/stores?latitude=#{storeLatitude}&longitude=#{storeLongitude}&pageSize=1000&radius=10000000")
      .header("Authorization", "Bearer #{access_token}")
    )
      .exec(http("Get Product Stock")
        .get("estock/productStock?productCode=#{UGA}")
        .header("Authorization", "Bearer #{access_token}")
      )
  }

  def recommandation_traking() = {
    exec(session => {
      val UUID_V4 = randomUUID()
      session.set("UUID_V4", UUID_V4)
    })
      .exec { session =>
        println("UUID_V4 : " + session("UUID_V4").as[String])
        session
      }
      .exec(http("Recommendation Tracking")
        .post("recommendation/tracking")
        .header("Authorization", "Bearer #{access_token}")
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
      .exec(http("Recommendation for Additional")
        .post("recommendation")
        .header("Authorization", "Bearer #{access_token}")
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
      .exec(http("Recommendation for Similar")
        .post("recommendation")
        .header("Authorization", "Bearer #{access_token}")
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
  }


  val scnRechercheParENA = scenario("Test Perf Recherche EAN")
    .feed(jddDataPVE)
    .feed(jddDataUGA)
    .exec(Autentication())
    .exec(getInfosVendeur())
    .exec(getInfosMagasin())
    .feed(jddDataEAN)
    .exec(get_Product_By_EAN())
    .exec(tracking_page())
    .exec(get_Additional_Recommandation())
    .exec(get_Similar_Recommandation())
    .exec(get_Produit_Stock())
    .exec(recommandation_traking())
  }
