package cz.zlehcito.model.dtos

data class RaceRoundPlayerState (
    val playerName: String,
    val points: Number,
    val mistakeCount: Number,
    val percentageDone: Number,
)

data class RaceRoundPlayerStateResponse (
    val raceGameRoundState: RaceRoundPlayerState
)


data class PersonalGameData (
    val idGame: Number,
    val idUser: String,
    var mistakeCount: Int,
    val correctCount: Int,
    val totalCount: Int,
)

data class RacePlayerResult (
    val inGameName: String,
    val points: Int,
)

data class RaceGameInterRoundState (
    val currentRound: Number,
    val playerResult: List<RacePlayerResult>
)

data class StartRaceRoundResponse (
    val raceGameInterRoundState: RaceGameInterRoundState
)

data class EndGameResponse(
    val racePlayerResults:  List<RacePlayerResult>
)

data class SubmitAnswerResponse(
    val termDefinitionPair: TermDefinitionPair,
    val answerCorrect: Boolean,
    val endOfRound: Boolean,
    val raceGameRoundState: RaceRoundPlayerState
)

data class NewTermResponse(
    val term: String
)
