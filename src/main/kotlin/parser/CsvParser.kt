package parser

import model.Player
import model.Position
import model.Team
import java.io.File

class CsvParser(val filename: String) {
    val teams =  mutableMapOf<String, Team>();
    val teamPlayers = mutableMapOf<String, MutableList<Player>>()
    val allPlayers = mutableListOf<Player>();
    var skippedCount = 0;

    fun parseFile() : Triple<List<Player>, List<Team>, Int> {
        parse();
        return Triple(allPlayers.toList(), teams.values.toList(), skippedCount);
    }

    fun parse() {
        val file = File(filename);
        file.useLines { lines ->
            for (line in lines.drop(1)) {
                val parts = line.split(";").map { it.trim() };
                val name = parts[0].ifBlank { null };
                val teamName = parts[1].ifBlank { null };
                val city = parts[2].ifBlank { null };
                val position = Position.fromCsvTokenOrNull(parts[3]);
                val nationality = parts[4].ifBlank { null };
                val agency = parts[5].ifBlank { null };
                val transferCost = parts[6].toIntOrNull();
                val participations = parts[7].toIntOrNull();
                val goals = parts[8].toIntOrNull();
                val assists = parts[9].toIntOrNull();
                val yellowCards = parts[10].toIntOrNull();
                val redCards = parts[11].toIntOrNull();

                if (name == null || teamName == null || city == null || position == null || nationality == null ||
                    transferCost == null || participations == null || goals == null ||
                    assists == null || yellowCards == null || redCards == null) {
                    skippedCount += 1;
                    continue;
                }

                val player = Player(
                    name,
                    position,
                    nationality,
                    agency,
                    transferCost,
                    participations,
                    goals,
                    assists,
                    yellowCards,
                    redCards);

                if (!teams.contains(teamName)) {
                    val team = Team(teamName, city);
                    teams[teamName] = team;
                }

                if (!teamPlayers.contains(teamName)) {
                    teamPlayers[teamName] = mutableListOf<Player>()
                }

                allPlayers.add(player);
                teamPlayers[teamName]?.add(player)
            }
        }

        for ((teamName, team) in teams) {
            team.players = teamPlayers[teamName]!!.toList()
        }
    }
}