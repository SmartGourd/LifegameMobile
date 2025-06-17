package cz.zlehcito.model

data class RaceGame (
    val idString: String,
    val name: String,
    val termDefinitionPairs: List<TermDefinitionPair>,
    val inputType: String,
    val roundCount: Int,
    val currentRound: Int,
    val players: List<Player>,
)

data class RaceGameRoundState (
    val playerName: String,
    val mistakeCount: Number,
    val percentageDone: Number,
)

data class TermDefinitionPair(val term: String, var definition: String)

data class RaceGameInterRoundState (
    val currentRound: Number,
    val playerResult: List<RacePlayerResult>
)

data class RacePlayerResult (
    val inGameName: String,
    val points: Int,
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
    val raceGameRoundState: RaceGameRoundState
)

data class NewTermResponse(
    val term: String
)
