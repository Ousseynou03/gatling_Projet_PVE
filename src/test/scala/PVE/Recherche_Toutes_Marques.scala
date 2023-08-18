package PVE

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure.ChainBuilder

import java.util.UUID.randomUUID

object Recherche_Toutes_Marques {

  private val FichierPath: String = System.getProperty("dataDir", "data/")
  private val FichierJddPVE: String = "jddPVE.csv"

  val jddDataPVE = csv(FichierPath + FichierJddPVE).circular


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

  ///////////////////////////////////////
  //// Recherche toutes les marques ////
  ///////////////////////////////////////
  def get_All_Brands() = {
    exec(http("Get All Brands")
      .get("brands")
      .header("Authorization", "Bearer #{access_token}")
    )
  }

  def get_OutOfStore_Brands() = {
    exec(http("Get OutOfStore Brands")
      .get("brands/outOfStore")
      .header("Authorization", "Bearer #{access_token}")
    )
  }

  def get_Store_Brands() = {
    exec(http("Get Store Brands")
      .get("brands/store")
      .header("Authorization", "Bearer #{access_token}")
    )
  }


  val scnRechercheParToutesMarques = scenario("TEST PERF Recherche par toutes marques")
    .feed(jddDataPVE)
    .exec(Autentication())
    .exec(getInfosVendeur())
    .exec(getInfosMagasin())
    .exec(get_All_Brands())
    .exec(get_OutOfStore_Brands())
    .exec(get_Store_Brands())


}
