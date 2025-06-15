package cz.zlehcito.model.entities

data class Game(
    val idGame: Int,
    val name: String,
    val playerCount: Int,
    val gameType: String,
)

data class GameResponse(
    val games: List<Game>
)