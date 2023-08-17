package PVE

import io.gatling.core.Predef._
import io.gatling.http.Predef._


class projetPVE extends Simulation{

  private val host : String = System.getProperty("urlCible", "https://astomlhp03.gmcloudhp.ggl.inet:1443/portailvendeur-server/api/")




  val httpProtocol = http.baseUrl(host)
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("fr,fr-FR;q=0.8,en-US;q=0.5,en;q=0.3")
    .userAgentHeader("TESTS-DE-PERF")




  val scnRechercheProduitParUGA = scenario("TEST PERF PROJET PVE PRODUIT UGA").exec(Recherche_Produit_UGA.scnRechercheParUGA)


  setUp(
    scnRechercheProduitParUGA.inject(atOnceUsers(1))
  ).protocols(httpProtocol)



}
