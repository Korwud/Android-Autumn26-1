package resolver

import model.Player
import model.Position
import model.Team
import parser.CsvParser

class Resolver(val players: MutableList<Player>, val teams: MutableList<Team>) : IResolver {
    override fun getCountWithoutAgency() : Int {
        return players.count { it.agency == null }
    }

    override fun getBestScorerDefender(): Pair<String, Int> {
        val bestDefender = players.filter { it.position.name == "DEFENDER" }.maxBy { it.goals }
        return bestDefender.name to bestDefender.goals
    }

    override fun getTheExpensiveGermanPlayerPosition(): String {
        return players.filter { it.nationality == "Germany" }.maxBy { it.transferCost }.position.position
    }

    override fun getTheRudestTeam(): Team {
        return teams.maxBy { team ->
            team.players.sumOf { it.redCards }.toDouble() / team.players.size
        }
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
            .sortedWith(compareByDescending<Player> { it.goals + 2 * it.assists }
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