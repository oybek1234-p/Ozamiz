package uz.oybek.ozamiz.ui.feature.menu.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.menu.model.MenuCategory
import uz.oybek.ozamiz.data.utils.DataWrapper
import com.google.gson.Gson
import uz.oybek.ozamiz.data.ai.AiChatRepository
import uz.oybek.ozamiz.data.ai.AiMenuRepository
import uz.oybek.ozamiz.data.ai.model.AiMenuResponse
import uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzerConfig
import uz.oybek.ozamiz.data.menu.MenuRepository
import uz.oybek.ozamiz.data.user.UserRepository

enum class MenuUiMode {
    INITIAL, INPUT, LOADING, RESULT
}

data class MenuListUiState(
    val primaryCategories: List<MenuCategory> = emptyList(),
    val secondaryCategories: List<MenuCategory> = emptyList(),
    val menus: List<Menu> = emptyList(),
    val selectedPrimaryId: String = "",
    val selectedSecondaryId: String = "",
    val isLoading: Boolean = false,
    val aiMenu: AiMenuResponse? = null,
    val uiMode: MenuUiMode = MenuUiMode.INITIAL,
    val fridgeFoods: String = "",
    val dislikedFoods: String = ""
)

class MenuListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MenuListUiState())
    val uiState = _uiState.asStateFlow()
    private var searchJob: Job? = null

    private var allMenus: List<Menu> = emptyList()
    private var staticMenusLoaded = false

    init {
        loadSavedAiMenu()
    }

    private fun loadSavedAiMenu() {
        AiMenuRepository.currentMenu.value?.let { json ->
            try {
                val menu = Gson().fromJson(json, AiMenuResponse::class.java)
                _uiState.value = _uiState.value.copy(aiMenu = menu, uiMode = MenuUiMode.RESULT)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(uiMode = MenuUiMode.INITIAL)
            }
        }
    }

    fun generateAiMenu(mode: String = "RANDOM") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(uiMode = MenuUiMode.LOADING, isLoading = true)
            val state = _uiState.value
            val user = UserRepository.user
            
            val prompt = FoodAnalyzerConfig.buildAiMenuPrompt(
                gender = user.gender,
                weight = user.weight,
                height = user.height,
                targetCalories = user.targetCalories,
                mode = mode,
                fridgeFoods = state.fridgeFoods,
                dislikedFoods = state.dislikedFoods
            )
            
            val result = AiChatRepository.sendMessage(
                message = prompt,
                systemInstruction = FoodAnalyzerConfig.AI_MENU_INSTRUCTION
            )
            
            if (result is DataWrapper.Success) {
                try {
                    val cleanJson = result.data.substringAfter("{").substringBeforeLast("}")
                    val json = "{$cleanJson}"
                    val menu = Gson().fromJson(json, AiMenuResponse::class.java)
                    _uiState.value = _uiState.value.copy(
                        aiMenu = menu, 
                        isLoading = false, 
                        uiMode = MenuUiMode.RESULT
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isLoading = false, uiMode = MenuUiMode.INITIAL)
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, uiMode = MenuUiMode.INITIAL)
            }
        }
    }

    fun saveAiMenu(json: String, context: android.content.Context) {
        viewModelScope.launch {
            AiMenuRepository.saveMenu(context, json)
        }
    }

    fun updateFridgeFoods(foods: String) {
        _uiState.value = _uiState.value.copy(fridgeFoods = foods)
    }

    fun loadStaticMenusIfNeeded() {
        if (!staticMenusLoaded) {
            staticMenusLoaded = true
            loadCategories()
            loadMenus()
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            MenuRepository.getCategories().collectLatest { list ->
                val primary = list.filter { !it.isSecondary }
                val secondary = list.filter { it.isSecondary }
                
                var selectedPrimaryId = _uiState.value.selectedPrimaryId
                if (selectedPrimaryId.isEmpty() && primary.isNotEmpty()) {
                    selectedPrimaryId = primary.find { it.name.lowercase().contains("nonushta") }?.id ?: primary.first().id
                }

                var selectedSecondaryId = _uiState.value.selectedSecondaryId
                
                _uiState.value = _uiState.value.copy(
                    primaryCategories = primary,
                    secondaryCategories = secondary,
                    selectedPrimaryId = selectedPrimaryId,
                    selectedSecondaryId = selectedSecondaryId
                )
                applyFilters()
            }
        }
    }

    private fun loadMenus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            MenuRepository.getMenus().collectLatest { list ->
                allMenus = list
                applyFilters()
            }
        }
    }

    fun onPrimaryCategorySelected(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedPrimaryId = categoryId)
        applyFilters()
    }

    fun onSecondaryCategorySelected(categoryId: String) {
        val current = _uiState.value.selectedSecondaryId
        val next = if (current == categoryId) "" else categoryId
        _uiState.value = _uiState.value.copy(selectedSecondaryId = next)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = allMenus.filter { menu ->
            val matchesPrimary = state.selectedPrimaryId.isEmpty() || menu.categoryIds.contains(state.selectedPrimaryId)
            val matchesSecondary = state.selectedSecondaryId.isEmpty() || menu.categoryIds.contains(state.selectedSecondaryId)
            matchesPrimary && matchesSecondary
        }
        _uiState.value = _uiState.value.copy(menus = filtered, isLoading = false)
    }

    fun onSearchQuery(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            applyFilters()
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = MenuRepository.searchMenus(query)
            if (result is DataWrapper.Success) {
                val searchResult = result.data
                val state = _uiState.value
                val filtered = searchResult.filter { menu ->
                    val matchesPrimary = state.selectedPrimaryId.isEmpty() || menu.categoryIds.contains(state.selectedPrimaryId)
                    val matchesSecondary = state.selectedSecondaryId.isEmpty() || menu.categoryIds.contains(state.selectedSecondaryId)
                    matchesPrimary && matchesSecondary
                }
                _uiState.value = _uiState.value.copy(menus = filtered, isLoading = false)
            }
        }
    }

    fun refresh() {
        loadMenus()
    }

    val isAdmin: Boolean
        get() = UserRepository.user.admin
}
