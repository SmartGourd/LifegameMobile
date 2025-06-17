package cz.zlehcito.model

//
data class GetLobbyGamesResponse(
    val games: List<LobbyGameForList>
)

data class LobbyGameForList(
    val idString: String,
    val name: String,
    val playerCount: Int,
    val gameType: String,
)

data class GetLobbyGameResponse(
    val game: LobbyGameDetail
)

data class LobbyGameDetail(
    val idString: String,
    val name: String,
    val players: List<Player>,
)

data class Player(val inGameName: String)


data class GameKey(
    val idGame: String,
    val idUser: String,
    val keyType: String,
)

data class JoinGameResponse(
    val gameKeyPlayer: GameKey
)