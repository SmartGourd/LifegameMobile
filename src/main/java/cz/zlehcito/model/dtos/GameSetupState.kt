package cz.zlehcito.model.dtos

data class GameSetupState(
    val gameTypeSpecificSettings: GameSettings,
    val players: List<Player>,
    val termDefinitionPairs: List<TermDefinitionPair>,
    val idGame: Int,
    val name: String,
    val gameStatus: String,
    val gameType: String,
    val playerCount: Int,
    val maxPlayers: Int
)

data class GameSettings(
    val roundCount: Int,
    val currentRound: Int,
    val inputType: String
)

data class Player(val inGameName: String)

data class TermDefinitionPair(val term: String, val definition: String)

data class GameSetupResponse(
    val data: GameSetupData
)

data class GameSetupData(
    val game: GameSetupState
)

data class GameKey(
    val idGame: Int,
    val idUser: String,
    val keyType: String,
)

data class JoinGameResponse(
    val data: JoinGameData
)

data class JoinGameData(
    val gameKeyPlayer: GameKey
)