package cz.zlehcito.model.dtos

class RaceRoundPlayerState (
    val playerName: String,
    val points: Number,
    val mistakeCount: Number,
    val percentageDone: Number,
)

class RaceRoundPlayerStateResponseData (
    val raceRoundPlayerState: RaceRoundPlayerState
)

class RaceRoundPlayerStateResponse (
    val data: RaceRoundPlayerStateResponseData
)


class PersonalGameData (
    val idGame: Number,
    val idUser: String,
    val mistakeCount: Number,
    val connectedCount: Number,
    val totalCount: Number,
)

class RacePlayerResult (
    val inGameName: String,
    val points: Int,
)

class RaceGameInterRoundState (
    val currentRound: Number,
    val playerResult: List<RacePlayerResult>
)

class StartRaceRoundResponseData (
    val raceGameInterRoundState: RaceGameInterRoundState
)

class StartRaceRoundResponse (
    val data: StartRaceRoundResponseData
)

class EndGameResponseData(
    val playerResult:  List<RacePlayerResult>
)

class EndGameResponse(
    val data: EndGameResponseData
)
