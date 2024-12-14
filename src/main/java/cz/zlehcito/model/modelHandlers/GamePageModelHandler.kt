package cz.zlehcito.model.modelHandlers

import com.google.gson.Gson
import cz.zlehcito.model.appState.AppState
import cz.zlehcito.model.dtos.EndGameResponse
import cz.zlehcito.model.dtos.GameSetupResponse
import cz.zlehcito.model.dtos.GameSetupState
import cz.zlehcito.model.dtos.PersonalGameData
import cz.zlehcito.model.dtos.RacePlayerResult
import cz.zlehcito.model.dtos.RaceRoundPlayerState
import cz.zlehcito.model.dtos.RaceRoundPlayerStateResponse
import cz.zlehcito.model.dtos.StartRaceRoundResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class GamePageModelHandler (
    private val appState: AppState,
    private val navigateToPage: (String) -> Unit,
) {
    private val _gameSetupState = MutableStateFlow<GameSetupState?>(null)
    val gameSetupState: StateFlow<GameSetupState?> get() = _gameSetupState
    private val _personalGameData = MutableStateFlow<PersonalGameData?>(null)
    val personalGameData: StateFlow<PersonalGameData?> get() = _personalGameData
    private val _playerFinalResults = MutableStateFlow<List<RacePlayerResult>>(emptyList())
    val playerFinalResults: StateFlow<List<RacePlayerResult>> get() = _playerFinalResults
    /*
    private val _playerRoundState = MutableStateFlow<List<RaceRoundPlayerState>>(emptyList())
    val playerRoundStates: StateFlow<List<RaceRoundPlayerState>> get() = _playerRoundState
    */
    private val _showResults = MutableStateFlow<Boolean>(false)
    val showResults: StateFlow<Boolean> get() = _showResults
    private val _secondsOfCountDown = MutableStateFlow<Number>(0)
    val secondsOfCountdown: StateFlow<Number> get() = _secondsOfCountDown


    init {
        appState.webSocketManager.registerHandler("GET_GAME") { json ->
            _gameSetupState.value = parseGameSetupStateJson(json.toString())
        }

        /*
        appState.webSocketManager.registerHandler("RACE_ROUND_STATE") { json ->

        }
        */

        appState.webSocketManager.registerHandler("START_RACE_ROUND") { json ->
            //_playerFinalResults.value = parseStartRaceRoundJson(json.toString())
        }

        appState.webSocketManager.registerHandler("END_RACE") { json ->
            _playerFinalResults.value = parseEndGameResultsJson(json.toString())
        }

    }

    public fun sendRacePutRequest() {
        val personalGameDataRequest = JSONObject().apply {
            put("type", "RACE_PUT")
            put("data", personalGameData)
        }
        appState.webSocketManager.sendMessage(personalGameDataRequest)
    }

    private fun parseGameSetupStateJson(response: String): GameSetupState? {
        return try {
            val gson = Gson()
            val gameResponse = gson.fromJson(response, GameSetupResponse::class.java)
            gameResponse.data.game
        } catch (e: Exception) {
            gameSetupState.value
        }
    }

    /*
    private fun parsePlayerRoundStateJson(response: String): RaceRoundPlayerState{
        return try {
            val gson = Gson()
            val endGameResponse = gson.fromJson(response, RaceRoundPlayerStateResponse::class.java)
            endGameResponse.data.raceRoundPlayerState
        } catch (e: Exception) {
            RaceRoundPlayerState("",0,0,0)
        }
    }

    private fun parseStartRaceRoundJson(response: String): List<RacePlayerResult>{
        return try {
            val gson = Gson()
            val startRaceRoundResponse = gson.fromJson(response, StartRaceRoundResponse::class.java)
            startRaceRoundResponse.data.raceGameInterRoundState.playerResult
        } catch (e: Exception) {
            playerFinalResults.value
        }
    }
    */

    private fun parseEndGameResultsJson(response: String): List<RacePlayerResult>{
        return try {
            val gson = Gson()
            val endGameResponse = gson.fromJson(response, EndGameResponse::class.java)
            endGameResponse.data.playerResult
        } catch (e: Exception) {
            playerFinalResults.value
        }
    }
}