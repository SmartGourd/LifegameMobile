package cz.zlehcito.model.entities

data class RaceRoundPlayerState (
    val playerName: String,
    val points: Number,
    val mistakeCount: Number,
    val percentageDone: Number,
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
