import model.Player
import model.Position
import model.Team
import parser.CsvParser
import resolver.Resolver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// Пример теста: так размечаются тесты и загружаются ресурсы из classpath.
// Дополните проект тестами на CsvParser (правила маппинга) и IResolver (задачи 1–8).
class InfrastructureTest {

    @Test
    fun `dirty dataset is available on classpath`() {
        assertNotNull(javaClass.classLoader.getResourceAsStream("fakePlayersDirty.csv"))
    }

    @Test
    fun `clean dataset for tests is available on classpath`() {
        assertNotNull(javaClass.classLoader.getResourceAsStream("fakePlayers.csv"))
    }

    @Test
    fun `position mapping works case-insensitive`() {
        assertEquals(Position.FORWARD, Position.fromCsvTokenOrNull("forward"))
        assertEquals(Position.FORWARD, Position.fromCsvTokenOrNull("Forward"))
        assertEquals(Position.FORWARD, Position.fromCsvTokenOrNull("FORWARD"))
        assertEquals(Position.FORWARD, Position.fromCsvTokenOrNull("fOrWaRd"))
        assertEquals(Position.DEFENDER, Position.fromCsvTokenOrNull("   DeFendER    "))
    }

    @Test
    fun `position returns null for unknown tokens`() {
        assertNull(Position.fromCsvTokenOrNull("bebebe"))
        assertNull(Position.fromCsvTokenOrNull("blank"))
        assertNull(Position.fromCsvTokenOrNull(""))
        assertNull(Position.fromCsvTokenOrNull("     "))
        assertNull(Position.fromCsvTokenOrNull("defener"))
    }

    @Test
    fun `empty agency does not skip player`() {
        val content = """
            Name;Team;City;Position;Nationality;Agency;Transfer cost;Participations;Goals;Assists;Yellow cards;Red cards
            James EmptyGuy;Barselona;Barcelona;forward;Spain;;456789;32;4;12;0;0
            Jared BlankGuy;Barselona;Barcelona;forward;Spain;   ;456789;32;4;12;0;0
        """.trimIndent()

        val file = File.createTempFile("test", ".csv")
        file.writeText(content)

        val result = CsvParser(file.path).parseFile()
        val players = result.first
        val skippedCount = result.third

        assertEquals(2, players.size)
        assertEquals(0, skippedCount)
        assertNull(players[0].agency)
        assertNull(players[1].agency)

        file.delete()
    }

    @Test
    fun `spaces in values does not skip player`() {
        val content = """
            Name;Team;City;Position;Nationality;Agency;Transfer cost;Participations;Goals;Assists;Yellow cards;Red cards
            James SpaceGuy         ;   Barselona    ;Barcelona      ; forward;   Spain;   ;456789   ;32  ;   4;12  ;  0;  0
        """.trimIndent()

        val file = File.createTempFile("test", ".csv")
        file.writeText(content)

        val result = CsvParser(file.path).parseFile()
        val players = result.first
        val skippedCount = result.third

        assertEquals(1, players.size)
        assertEquals(0, skippedCount)
        val player = Player("James SpaceGuy", Position.FORWARD, "Spain", null, 456789, 32, 4, 12, 0, 0)
        assertEquals(players[0], player)

        file.delete()
    }

    @Test
    fun `empty string values skip player`() {
        val content = """
            Name;Team;City;Position;Nationality;Agency;Transfer cost;Participations;Goals;Assists;Yellow cards;Red cards
            ;Barselona;Barcelona;forward;Spain;;456789;32;4;12;0;0
            James EmptyGuy;;Barcelona;forward;Spain;;456789;32;4;12;0;0
            James EmptyGuy;Barselona;;forward;Spain;;456789;32;4;12;0;0
            James EmptyGuy;Barselona;Barcelona;;Spain;;456789;32;4;12;0;0
            James EmptyGuy;Barselona;Barcelona;forward;;;456789;32;4;12;0;0
        """.trimIndent()

        val file = File.createTempFile("test", ".csv")
        file.writeText(content)

        val result = CsvParser(file.path).parseFile()
        val players = result.first
        val skippedCount = result.third

        assertEquals(0, players.size)
        assertEquals(5, skippedCount)

        file.delete()
    }

    @Test
    fun `empty numeric values skip player`() {
        val content = """
            Name;Team;City;Position;Nationality;Agency;Transfer cost;Participations;Goals;Assists;Yellow cards;Red cards
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;;32;4;12;0;0
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;456789;;4;12;0;0
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;456789;32;;12;0;0
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;456789;32;4;;0;0
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;456789;32;4;12;;0
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;456789;32;4;12;0;
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;;;;;;
        """.trimIndent()

        val file = File.createTempFile("test", ".csv")
        file.writeText(content)

        val result = CsvParser(file.path).parseFile()
        val players = result.first
        val skippedCount = result.third

        assertEquals(0, players.size)
        assertEquals(7, skippedCount)

        file.delete()
    }

    @Test
    fun `non-numeric values skip player`() {
        val content = """
            Name;Team;City;Position;Nationality;Agency;Transfer cost;Participations;Goals;Assists;Yellow cards;Red cards
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;blabla;32;4;12;0;0
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;456789;blabla;4;12;0;0
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;456789;32;blabla;12;0;0
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;456789;32;4;blabla;0;0
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;456789;32;4;12;blabla;0
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;456789;32;4;12;0;blabla
            James EmptyGuy;Barselona;Barcelona;forward;Spain;Agency;pupupu;cat;indigo;why;test;blabla
        """.trimIndent()

        val file = File.createTempFile("test", ".csv")
        file.writeText(content)

        val result = CsvParser(file.path).parseFile()
        val players = result.first
        val skippedCount = result.third

        assertEquals(0, players.size)
        assertEquals(7, skippedCount)

        file.delete()
    }

    private fun makePlayers() : MutableList<Player> {
        return mutableListOf(
            Player("Player 1", Position.DEFENDER, "Spain", "88005553535", 250000, 32, 4, 2, 0, 3),
            Player("Player 2", Position.DEFENDER, "Germany", null, 250000, 32, 35, 13, 0, 0),
            Player("Player 3", Position.FORWARD, "Germany", null, 15000000, 32, 75, 32, 0, 1),
            Player("Player 4", Position.GOALKEEPER, "USA", null, 850000, 32, 35, 26, 0, 4),
            Player("Player 5", Position.GOALKEEPER, "USA", "Fufelshmerz", 650000, 32, 5, 63, 0, 1),
            Player("Player 6", Position.MIDFIELD, "Germany", "PerryThePlatypus", 650000, 32, 25, 23, 0, 2),
            Player("Player 7", Position.MIDFIELD, "USA", "PerryThePlatypus", 50000, 32, 2, 3, 0, 11)
        )
    }

    private fun makeTeams(players: List<Player>) : MutableList<Team> {
        val team1 = Team("Team1", "Berlin", players.subList(0, 2).toMutableList())
        val team2 = Team("Team2", "New-York", players.subList(2, 5).toMutableList())
        val team3 = Team("Team3", "Berlin", players.subList(5, 7).toMutableList())
        return mutableListOf(team1, team2, team3)
    }

    private fun makeResolver() : Resolver {
        val players = makePlayers()
        val teams = makeTeams(players)
        return Resolver(players, teams)
    }

    @Test
    fun `resolver returns count without agency`() {
        val resolver = makeResolver()
        val count = resolver.getCountWithoutAgency()
        assertEquals(3, count)
    }

    @Test
    fun `resolver returns best defender and goal count`() {
        val resolver = makeResolver()
        val (name, goals) = resolver.getBestScorerDefender()
        assertEquals("Player 2", name)
        assertEquals(35, goals)
    }

    @Test
    fun `resolver returns the expensive German position`() {
        val resolver = makeResolver()
        val position = resolver.getTheExpensiveGermanPlayerPosition()
        assertEquals("Нападающий", position)
    }

    @Test
    fun `resolver returns the rudest team`() {
        val resolver = makeResolver()
        val teams = makeTeams(makePlayers())
        val rudeTeam = resolver.getTheRudestTeam()
        assertEquals(teams[2], rudeTeam)
    }

    @Test
    fun `resolver returns average cost by position`() {
        val resolver = makeResolver()
        val costByPosition = resolver.getAverageTransferCostByPosition()
        val answer = mapOf(
            Position.FORWARD to 15000000.0,
            Position.GOALKEEPER to 750000.0,
            Position.MIDFIELD to 350000.0,
            Position.DEFENDER to 250000.0
        )
        assertEquals(answer, costByPosition)
    }

    @Test
    fun `resolver returns the most valuable players`() {
        val resolver = makeResolver()
        val valuablePlayers = resolver.getMostValuablePlayers()
        val players = makePlayers()
        val answer = mutableListOf(players[2], players[4], players[3])
        assertEquals(answer, valuablePlayers)
    }

    @Test
    fun `resolver returns the most popular agency`() {
        val resolver = makeResolver()
        val popularAgency = resolver.getMostPopularAgencyByCountry()
        val answer = mapOf(
            "Spain" to "88005553535",
            "USA" to "Fufelshmerz",
            "Germany" to "PerryThePlatypus"
        )
        assertEquals(answer, popularAgency)
    }

    @Test
    fun `resolver returns goals by median cost`() {
        val resolver = makeResolver()
        val medianByCost = resolver.getGoalsShareByMedianCost()

        val players = makePlayers()
        val sorted = players.map { it.transferCost }.sorted()
        val median = sorted[sorted.size / 2].toDouble()

        val totalGoals = players.sumOf { it.goals }
        val cheapGoals = players.filter { it.transferCost <= median }.sumOf { it.goals }
        val expensiveGoals = totalGoals - cheapGoals

        val answer = Pair(cheapGoals.toDouble() / totalGoals, expensiveGoals.toDouble() / totalGoals)

        assertEquals(answer, medianByCost)
    }
}
