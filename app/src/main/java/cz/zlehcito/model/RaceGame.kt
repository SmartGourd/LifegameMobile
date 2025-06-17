package cz.zlehcito.model

data class GetRaceGameResponse(
    val game: RaceGame
)

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
    val mistakeCount: Int,
    val percentageDone: Int,
)

data class TermDefinitionPair(val term: String, var definition: String)

data class RaceGameInterRoundState (
    val currentRound: Int,
    val playerResults: List<RacePlayerResult>
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
