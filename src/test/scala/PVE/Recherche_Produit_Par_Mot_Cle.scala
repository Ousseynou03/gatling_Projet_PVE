package PVE

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure.ChainBuilder

import java.util.UUID.randomUUID
object Recherche_Produit_Par_Mot_Cle {

  private val FichierPath: String = System.getProperty("dataDir", "data/")
  private val FichierJddPVE: String = "jddPVE.csv"
  private val FichierJddUGA: String = "jddUGA.csv"
  private val FichierJddEAN: String = "jddEAN.csv"
  private val FichierJddCustomer : String = "jddCustomer.csv"

  val jddDataPVE = csv(FichierPath + FichierJddPVE).circular
  val jddDataEAN = csv(FichierPath + FichierJddEAN).circular
  val jddDataUGA = csv(FichierPath + FichierJddUGA).circular
  val jddDataCustomer = csv(FichierPath + FichierJddCustomer).circular



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


  //Récupération informations vendeur

  def getInfosVendeur() = {
    exec(http("Get_infos_vendeur")
      .get("auth/login/employee")
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("#.badgeNumber").saveAs("user_BadgeNumber"))
      .check(jsonPath("#.storeCode").saveAs("user_StoreCode"))
      .check(status.is(200)))
  }


  //Récupération des infos magasin vendeur
  def getInfosMagasin() = {
    exec(http("Get_infos_Magasin")
      .get("sap/stores/storeCode/#{user_StoreCode}")
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("#.sourceLatitude").saveAs("storeLatitude"))
      .check(jsonPath("#.sourceLongitude").saveAs("storeLongitude"))
      .check(status.is(200)))
  }

  ///////////////////////////////////////
  // RECHERCHE PRODUIT PAR MOT CLÉ
  //////////////////////////////////////

  def get_Basic_Colors() = {
    exec(http("Search Products by Keyword")
      .get("sap/products/search?query=#{SEARCH_TERM}&currentPage=0")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("#.products[*].basicColor").findAll.saveAs("basicColors"))
    )
      .exec { session =>
        val colors = session("basicColors").as[Seq[String]]
        println("Basic Colors: " + colors.mkString(", "))
        session
      }
  }

  def get_Tracking_Page_ID() = {
    exec(http("Get Tracking Page ID")
      .get("recoT2sPageId?brand=GL&recoType=TRACKING&pageScope=PRODUCT_RESULT")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("#.idT2sPage").saveAs("idT2sPageTracking"))
    )
  }

  def recommendation_Tracking() = {
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
        // à vérifier  |"iID":[#{productIds}],
          |"eN":"VIEW"
          |}
          |""".stripMargin)).asJson
    )
  }


  val scnRechercheProduitMotCle = scenario("Test Perf Recherche Mot Clé")
    .feed(jddDataPVE)
    .exec(Autentication())
    .exec(getInfosVendeur())
    .exec(getInfosMagasin())
    .feed(jddDataCustomer)
    .exec(get_Basic_Colors())
    .exec(get_Tracking_Page_ID())
    .exec(recommendation_Tracking())







}
