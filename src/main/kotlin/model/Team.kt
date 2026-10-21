package model

data class Team(
    val name: String,
    val city: String,
    var players: List<Player> = listOf<Player>()
)