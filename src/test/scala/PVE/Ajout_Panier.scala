package PVE


import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure.ChainBuilder

import java.util.UUID.randomUUID

object Ajout_Panier{

  private val FichierPath: String = System.getProperty("dataDir", "data/")
  private val FichierJddPVE: String = "jddPVE.csv"
  private val FichierJddUGA: String = "jddUGA.csv"
  private val FichierJddCustomer: String = "jddCustomer.csv"

  val jddDataPVE = csv(FichierPath + FichierJddPVE).circular
  val jddDataCustomer = csv(FichierPath + FichierJddCustomer).circular
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

  //////////////////////////////////////
  //// Ajouter au panier
  ////////////////////////////////////

  def create_Cart() = {
    exec(http("Create Cart")
      .post("{sap/anonymous/carts")
      .header("Authorization", "Bearer #{access_token}")
      .body(StringBody("{}")).asJson
      .check(jsonPath("$.guid").saveAs("cart_Guid"))
    )
  }

  def add_To_Cart() = {
    exec(http("Add to Cart")
      .post("sap/anonymous/carts/#{cart_Guid}/entries")
      .header("Authorization", "Bearer #{access_token}")
      .body(StringBody(
        """
          |{
          |"product":
          |{
          |"code":"#{UGA}"
          |},
          |"quantity":1
          |}
          |""".stripMargin)).asJson
    )
  }

  def validate_Cart() = {
    exec(http("Validate Cart")
      .post("sap/anonymous/carts/#{cart_Guid}/validate")
      .header("Authorization", "Bearer #{access_token}")
    )
  }

  def search_Client() = {
    exec(http("Search Client")
      .post("client/searchClients")
      .header("Authorization", "Bearer #{access_token}")
      .body(StringBody(
        """
          |{
          |"size":11,
          |"search":
          |{
          |"phonetic":true,
          |"advanced":
          |{
          |"Mail":"#{CUSTOMER_MAIL}"
          |}
          |},
          |"filters":{
          |"EnseignePersonne":
          |{
          |"type":"terms",
          |"attributes":["GL"]
          |}
          |}
          |}""".stripMargin)).asJson
      .check(jsonPath("$.hits[0].source.Id").saveAs("customer_Num_Fid"))
    )
  }

  def get_Client_Info() = {
    exec(http("Get Client Info")
      .get("client/fid/#{customer_Num_Fid}")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("$.Customer.PersonalInfo.Postal.Id").saveAs("CUSTOMER_ADDRESS_ID"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.Type").saveAs("CUSTOMER_ADDRESS_TYPE"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.AddressLine1").saveAs("CUSTOMER_ADDRESS_LINE_1"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.AddressLine2").saveAs("CUSTOMER_ADDRESS_LINE_2"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.CivilityCode").saveAs("CUSTOMER_ADDRESS_CIVILITY_CODE"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.City").saveAs("CUSTOMER_CITY"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.PostalCode").saveAs("CUSTOMER_ADDRESS_POSTAL_CODE"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.CountryCode").saveAs("CUSTOMER_ADDRESS_COUNTRY_CODE"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.IsDelivery").saveAs("CUSTOMER_ADDRESS_IS_DELIVERY"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.IsBilling").saveAs("CUSTOMER_ADDRESS_IS_BILLING"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.Npai").saveAs("CUSTOMER_ADDRESS_NPAI"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.Company").saveAs("CUSTOMER_ADDRESS_COMPANY"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.Firstname").saveAs("CUSTOMER_ADDRESS_FIRSTNAME"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.Name").saveAs("CUSTOMER_ADDRESS_NAME"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.Label").saveAs("CUSTOMER_ADDRESS_LABEL"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.PhoneNumber").saveAs("CUSTOMER_ADDRESS_PHONENUMBER"))
      .check(jsonPath("$Customer.PersonalInfo.Postal.State").saveAs("CUSTOMER_ADDRESS_STATE"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.AddressLine3").saveAs("CUSTOMER_ADDRESS_LINE_3"))
      .check(jsonPath("$.Customer.PersonalInfo.Postal.AddressLine4").saveAs("CUSTOMER_ADDRESS_LINE_4"))
    )
  }

  def sync_GGL_Customer() = {
    exec(http("Sync GGL Customer")
      .get("ggl-customer/#{customer_Num_Fid}")
      .header("Authorization", "Bearer #{access_token}")
    )
  }

  def sync_Customer_SAP() = {
    exec(http("Sync Customer SAP")
      .get("sap/customer/#{CUSTOMER_MAIL}/sync/#{customer_Num_Fid}")
      .header("Authorization", "Bearer #{access_token}")
    )
  }

  ///////////////////////////////////////////////
  /// Informations Livraison /////
  ///////////////////////////////////////////////
  def select_Delivery_Mode() = {
    exec(http("Select Delivery Mode")
      .get("sap/#{CUSTOMER_MAIL}/carts/#{cart_Guid}/suborder/deliverymodes?entriesNumbers=0")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("$.deliveryModes[0].code").saveAs("DELIVERYMODE_CODE"))
    )
  }

  def select_Store() = {
    exec(http("Select Store")
      .get("sap/stores?pageSize=1000")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("$.stores[0].name").saveAs("STORE_NAME"))
    )
  }

  def select_Store_Address() = {
    exec(http("Select Store Address")
      .post("sap/#{CUSTOMER_MAIL}/cart/#{cart_Guid}/address/pointofservice?entriesNumbers=0&storeName=#{STORE_NAME}")
      .header("Authorization", "Bearer #{access_token}")
    )
  }


  //////////////////////////////////////////////////////////////////////
  // Choix du mode de paiement & passation de commande (taux d'arrêt 20%)
  //////////////////////////////////////////////////////////////////////


  def place_Order() = {
    exec(http("Place Order")
      .post("sap/#{CUSTOMER_MAIL}/orders/#{cart_Guid}?seller=#{user_BadgeNumber}&storeCode=#{user_StoreCode}")
      .header("Authorization", "Bearer #{access_token}")
      .header("Content-Type", "application/json")
      .body(StringBody(
        """
          |{
          |"Id":"#{CUSTOMER_ADDRESS_ID}",
          |"Type":"#{CUSTOMER_ADDRESS_TYPE}",
          |"AddressLine1":"#{CUSTOMER_ADDRESS_LINE_1}",
          |"AddressLine2":"#{CUSTOMER_ADDRESS_LINE_2}",
          |"CivilityCode":"#{CUSTOMER_ADDRESS_CIVILITY_CODE}",
          |"City":"#{CUSTOMER_CITY}",
          |"PostalCode":"#{CUSTOMER_ADDRESS_POSTAL_CODE}",
          |"CountryCode":"#{CUSTOMER_ADDRESS_COUNTRY_CODE}",
          |"IsDelivery":#{CUSTOMER_ADDRESS_IS_DELIVERY},
          |"IsBilling":#{CUSTOMER_ADDRESS_IS_BILLING},
          |"Npai":#{CUSTOMER_ADDRESS_NPAI},
          |"Company":#{CUSTOMER_ADDRESS_COMPANY},
          |"Firstname":"#{CUSTOMER_ADDRESS_FIRSTNAME}",
          |"Name":"#{CUSTOMER_ADDRESS_NAME}",
          |"Label":#{CUSTOMER_ADDRESS_LABEL},
          |"PhoneNumber":"#{CUSTOMER_ADDRESS_PHONENUMBER}",
          |"State":"#{CUSTOMER_ADDRESS_STATE}",
          |"AddressLine3":"#{CUSTOMER_ADDRESS_LINE_3}",
          |"AddressLine4":"#{CUSTOMER_ADDRESS_LINE_4}"
          |}
          |""".stripMargin)).asJson
    )
  }

  def confirm_Payment() = {
    exec(session => {
      val UUID = randomUUID()
      session.set("UUID", UUID)
    })
    .exec(http("Confirm Payment")
      .get("recoT2sPageId?brand=GL&recoType=TRACKING&pageScope=CONFIRM_PAYMENT")
      .header("Authorization", "Bearer #{access_token}")
      .check(jsonPath("$.idT2sPage").saveAs("idT2sPageConfirmPayment"))
    )
      .exec(http("Recommendation Tracking")
        .post("recommendation/tracking")
        .header("Authorization", "Bearer #{access_token}")
        .header("Content-Type", "application/json")
        .body(StringBody(
          """
            |{
            |"pID":"#{idT2sPageConfirmPayment}",
            |"tID":"#{UUID}",
            |"uEM":"#{customer_Num_Fid}",
            |"iID":["#{UGA}-${COLOR_VARIANT}"],
            |"bS":23455,
            |"qTE":[1],
            |"oID":"#{cart_Guid}",
            |"eN":"VIEW"
            |}
            |""".stripMargin)).asJson
      )
  }

  val scnAjoutPanier = scenario("Test Perf Ajout Panier")
    .feed(jddDataPVE)
    .exec(Autentication())
    .exec(getInfosVendeur())
    .exec(getInfosMagasin())
    .exec(create_Cart())
    .feed(jddDataUGA)
    .exec(add_To_Cart())
    .exec(validate_Cart())
    .feed(jddDataCustomer)
    .exec(search_Client())
    .exec(get_Client_Info())
    .exec(sync_GGL_Customer())
    .exec(sync_Customer_SAP())
    .exec(select_Delivery_Mode())
    .exec(select_Store())
    .exec(select_Store_Address())
    .exec(place_Order())
    .exec(confirm_Payment())

}
