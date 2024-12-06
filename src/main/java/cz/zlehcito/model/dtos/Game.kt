package cz.zlehcito.model.dtos

data class Game(
    val idGame: Int,
    val name: String,
    val playerCount: Int,
    val maxPlayers: Int,
    val gameType: String,
    val gameStatus: String
)

data class GameResponse(
    val data: GameData
)

data class GameData(
    val games: List<Game>
)