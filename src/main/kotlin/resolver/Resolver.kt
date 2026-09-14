package resolver

import model.Player
import model.Position
import model.Team
import parser.CsvParser

class Resolver(val players: MutableList<Player>, val teams: MutableList<Team>) : IResolver {
    override fun getCountWithoutAgency() : Int {
        var count = 0;
        for (player in players) {
            if (player.agency == null) {
                count += 1;
            }
        }
        return count;
    }

    override fun getBestScorerDefender(): Pair<String, Int> {
        var defender = "";
        var goals = 0;
        for (player in players) {
            if (player.position.position == "Защитник" && player.goals > goals) {
                defender = player.name;
                goals = player.goals;
            }
        }
        return Pair(defender, goals);
    }

    override fun getTheExpensiveGermanPlayerPosition(): String {
        var position = "";
        var sum = 0;
        for (player in players) {
            if (player.nationality == "Germany" && player.transferCost > sum) {
                position = player.position.position;
                sum = player.transferCost;
            }
        }
        return position;
    }

    override fun getTheRudestTeam(): Team {
        val rudeTeams = mutableMapOf<Team, Double>();
        for (team in teams) {
            var redCardsCount = 0;
            for (player in team.players) {
                redCardsCount += player.redCards;
            }
            rudeTeams[team] = (redCardsCount / team.players.size).toDouble();
        }
        return rudeTeams.maxBy { it.value }.key;
    }

    override fun getAverageTransferCostByPosition(): Map<Position, Double> {
        val sorted: Map<Position, Double> = players
            .groupBy { it.position }
            .mapValues { (_, group) -> group.sumOf { it.transferCost.toLong() }.toDouble() / group.size }
            .entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
        return sorted;
    }

    override fun getMostValuablePlayers(): List<Player> {
        val top3 = players
            .sortedWith(
                compareByDescending<Player> { it.goals + 2 * it.assists }
                    .thenBy { it.name }
            )
            .take(3);
        return top3;
    }

    override fun getMostPopularAgencyByCountry(): Map<String, String> {
        val result: Map<String, String> = players
            .filter { it.agency != null }
            .groupBy { it.nationality }
            .mapValues { (_, group) ->
                group.groupingBy { it.agency }
                    .eachCount()
                    .entries
                    .sortedWith(
                        compareByDescending<Map.Entry<String?, Int>> { it.value }
                            .thenBy { it.key }
                    )
                    .firstOrNull()
                    ?.key as String
            }
        return result
    }

    override fun getGoalsShareByMedianCost(): Pair<Double, Double> {
        val sorted = players.map { it.transferCost }.sorted()
        var median = 0.0;
        if (sorted.size % 2 == 1){
            median = sorted[sorted.size / 2].toDouble();
        }
        else {
            median = (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0;
        }

        val totalGoals = players.sumOf { it.goals }
        val cheapGoals = players.filter { it.transferCost <= median }.sumOf { it.goals }
        val expensiveGoals = totalGoals - cheapGoals

        return Pair(cheapGoals.toDouble() / totalGoals, expensiveGoals.toDouble() / totalGoals)
    }
}