package cz.zlehcito.model.dtos

data class GameSetupState(
    val players: List<Player>,
    val termDefinitionPairs: List<TermDefinitionPair>,
    val idGame: Int,
    val name: String,
    val gameStatus: String,
    val maxPlayers: Int,
    val roundCount: Int,
    val currentRound: Int,
    val inputType: String
)

data class Player(val inGameName: String)

data class TermDefinitionPair(val term: String, var definition: String)

data class GameSetupResponse(
    val game: GameSetupState
)

data class GameKey(
    val idGame: Int,
    val idUser: String,
    val keyType: String,
)

data class JoinGameResponse(
    val gameKeyPlayer: GameKey
)