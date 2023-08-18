package PVE

import io.gatling.core.Predef._
import io.gatling.http.Predef._


class projetPVE extends Simulation{

  private val host : String = System.getProperty("urlCible", "https://astomlhp03.gmcloudhp.ggl.inet:1443/portailvendeur-server/api/")




  val httpProtocol = http.baseUrl(host)
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("fr,fr-FR;q=0.8,en-US;q=0.5,en;q=0.3")
    .userAgentHeader("TESTS-DE-PERF-PROJET-PVE")




  val scnRechercheProduitParUGA = scenario("TEST PERF PROJET PVE PRODUIT UGA").exec(Recherche_Produit_UGA.scnRechercheParUGA)
  val scnRechercheProduitParEAN = scenario("TEST PERF PROJET PVE PRODUIT EAN").exec(Recherche_Produit_EAN.scnRechercheParENA)
  val scnRechercheParMotCle = scenario("TEST PERF RECHERCHE MOT CLE").exec(Recherche_Produit_Par_Mot_Cle.scnRechercheProduitMotCle)
  val scnRechercheCatalogue = scenario("TEST PERF RECHERCHE CATALOGUE").exec(Recherche_Catalogue.scnRechercheCatalogue)
  val scnAjoutAuPanier = scenario("TEST PERF AJOUT AU PANIER").exec(Ajout_Panier.scnAjoutPanier)
  val scnRechercheToutesLesMarques = scenario("TEST PERF RECHERCHE PAR TOUTES LES MARQUES").exec(Recherche_Toutes_Marques.scnRechercheParToutesMarques)
  val scnPastillage = scenario("TEST PERTF PASTILLAGE").exec(Pastillage.scnRechercheParUGA)

  setUp(
    scnRechercheProduitParUGA.inject(atOnceUsers(1)),
    scnRechercheProduitParEAN.inject(atOnceUsers(1)),
    scnRechercheParMotCle.inject(atOnceUsers(1)),
    scnRechercheCatalogue.inject(atOnceUsers(1)),
    scnAjoutAuPanier.inject(atOnceUsers(1)),
    scnRechercheToutesLesMarques.inject(atOnceUsers(1)),
    scnPastillage.inject(atOnceUsers(1)),

  ).protocols(httpProtocol)



}
