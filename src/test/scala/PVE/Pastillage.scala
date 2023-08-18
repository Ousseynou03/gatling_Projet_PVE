package PVE

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure.ChainBuilder

import java.util.UUID.randomUUID
object Pastillage {

  private val FichierPath: String = System.getProperty("dataDir", "data/")
  private val FichierJddPVE: String = "jddPVE.csv"
  private val FichierJddUGA: String = "jddUGA.csv"
  val jddDataPVE = csv(FichierPath + FichierJddPVE).circular
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

  ////////////////////////////////////////
  //Pastillage ///////////////////
  ////////////////////////////////////

  def get_Pastillage() = {
    exec(http("Get Pastillage")
      .get("v1/pastillage/#{UGA}")
      .header("Authorization", "Bearer #{access_token}")
    )
  }

  def get_Product_Stock() = {
    exec(http("Get Product Stock")
      .get("estock/productStock?productCode=${UGA}")
      .header("Authorization", "Bearer #{access_token}")
    )
  }

  val scnRechercheParUGA = scenario("TEST PERF Pastillage")
    .feed(jddDataPVE)
    .feed(jddDataUGA)
    .exec(Autentication())
    .exec(getInfosVendeur())
    .exec(getInfosMagasin())
    .exec(get_Pastillage())
    .exec(get_Product_Stock())

}
