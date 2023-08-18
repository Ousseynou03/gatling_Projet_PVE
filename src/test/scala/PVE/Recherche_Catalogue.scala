package PVE

import io.gatling.core.Predef._
import io.gatling.http.Predef._


object Recherche_Catalogue {

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


  ////////////////////////////////////////
  //// Recherche Catalogue
  /////////////////////////////////////


  def search_Catalog_First_Level() = {
    exec(http("Search Catalog First Level")
      .get("sap/products/search?query=&currentPage=0")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("#.facets[?(@.code == 'categories')].values[0].query.query.value").saveAs("QUERY_VALUE"))
    )
  }

  def search_Catalog_Next_Level() = {
    exec(http("Search Catalog Next Level")
      .get("sap/products/search?query=#{QUERY_VALUE(current)}&currentPage=0")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("#.facets[?(@.code == 'categories')].values[0].query.query.value").saveAs("QUERY_VALUE(next)"))
    )
  }

  def search_Catalog_Last_Level() = {
    exec(http("Search Catalog Last Level")
      .get("sap/products/search?query=#{QUERY_VALUE(last)}&currentPage=0")
      .header("Authorization", "Bearer #{access_token}")
    )
  }


  val scnRechercheCatalogue = scenario("Test Perf Recherche Catalogue")
    .feed(jddDataPVE)
    .exec(Autentication())
    .exec(getInfosVendeur())
    .exec(getInfosMagasin())
    .exec(search_Catalog_First_Level())
    .exec(search_Catalog_Next_Level())
    .exec(search_Catalog_Last_Level())

}
