package uz.oybek.ozamiz.ui.foods

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.foods.FoodCategory
import uz.oybek.ozamiz.data.foods.FoodCategoryRepository
import uz.oybek.ozamiz.data.foods.Recipe
import uz.oybek.ozamiz.data.foods.RecipeRepository
import uz.oybek.ozamiz.handleException

data class AddEditRecipeUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val availableCategories: List<FoodCategory> = emptyList(),
    val recipeToEdit: Recipe? = null,
    val selectedCategoryIdsInternal: List<String> = emptyList(), // ViewModel ichki holati uchun
    val title: String = "Yangi Retsept Qo'shish",
    val canDelete: Boolean = false,
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false
)

class AddEditRecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val recipeRepository = RecipeRepository()
    private val categoryRepository = FoodCategoryRepository()
    private val gson = Gson()
    private val TAG = "AddEditRecipeVM"

    private val _uiState = MutableLiveData(AddEditRecipeUiState())
    val uiState: LiveData<AddEditRecipeUiState> = _uiState

    // Saqlash uchun ASOSIY retsept holati
    private var currentRecipe = Recipe()

    // Fragmentdan kelgan tanlangan kategoriyalarni VAQTINCHALIK saqlash uchun
    private var tempSelectedCategoryIds: List<String> = emptyList()

    var selectedImageUri: Uri? = null
    private var initialRecipeJson: String? = null

    fun initialize(recipeJson: String?) {
        initialRecipeJson = recipeJson
        loadAvailableCategories()

        if (recipeJson != null) {
            try {
                currentRecipe = gson.fromJson(recipeJson, Recipe::class.java)
                tempSelectedCategoryIds =
                    currentRecipe.categoryIds ?: emptyList() // Boshlang'ich IDlarni saqlash
                _uiState.value = _uiState.value?.copy(
                    recipeToEdit = currentRecipe,
                    selectedCategoryIdsInternal = tempSelectedCategoryIds, // UI uchun boshlang'ich IDlar
                    title = "Retseptni Tahrirlash",
                    canDelete = true
                )
                Log.d(TAG, "Initialized for editing. Initial categoryIds: $tempSelectedCategoryIds")
            } catch (e: Exception) {
                handleException(e, "RecipeJsonParseError")
                _uiState.value = _uiState.value?.copy(
                    isLoading = false, errorMessage = "Retsept ma'lumotlarini o'qishda xatolik"
                )
            }
        } else {
            currentRecipe = Recipe()
            tempSelectedCategoryIds = emptyList()
            _uiState.value = _uiState.value?.copy(
                recipeToEdit = null,
                selectedCategoryIdsInternal = emptyList(),
                title = "Yangi Retsept Qo'shish",
                canDelete = false
            )
            Log.d(TAG, "Initialized for adding new recipe.")
        }
        _uiState.value = _uiState.value?.copy(isLoading = true) // Kategoriyalar yuklanishini kutish
    }

    private fun loadAvailableCategories() {
        Log.d(TAG, "Loading available categories...")
        viewModelScope.launch {
            categoryRepository.getAllCategories().catch { e ->
                Log.e(TAG, "Error loading categories", e)
                handleException(e as Exception, "LoadCategoriesForRecipe")
                _uiState.postValue(
                    _uiState.value?.copy(
                        isLoading = false, // Yuklanish tugadi (xato bilan)
                        errorMessage = "Kategoriyalarni yuklashda xatolik"
                    )
                )
            }.collectLatest { result ->
                when (result) {
                    is DataWrapper.Success -> {
                        Log.d(
                            TAG, "Categories loaded successfully: ${result.data.size} categories"
                        )
                        _uiState.postValue(
                            _uiState.value?.copy(
                                isLoading = false, // Yuklanish tugadi
                                availableCategories = result.data
                            )
                        )
                    }

                    is DataWrapper.Error -> _uiState.postValue(
                        _uiState.value?.copy(
                            isLoading = false, errorMessage = result.error
                        )
                    )

                    is DataWrapper.Loading -> _uiState.postValue(_uiState.value?.copy(isLoading = true))
                }
            }
        }
    }

    // Fragmentdagi har bir maydon o'zgarganda chaqiriladi (kategoriyalardan tashqari)
    fun updateRecipeField(updateAction: Recipe.() -> Recipe) {
        currentRecipe = currentRecipe.updateAction()
        // Bu yerda UI state'ni yangilash shart emas, chunki UI EditText'lardan o'qiydi
        // Lekin real-time validation uchun kerak bo'lishi mumkin
        Log.d(TAG, "updateRecipeField called. Current recipe name: ${currentRecipe.name}")
    }

    // Fragmentdagi KATEGORIYA chiplari o'zgarganda chaqiriladi
    fun updateSelectedCategories(selectedIds: List<String>) {
        Log.d(TAG, "updateSelectedCategories called with IDs: $selectedIds")
        tempSelectedCategoryIds = selectedIds // Vaqtinchalik listni yangilash
        // UI state'ni ham yangilash (agar chipGroup'ni to'g'ridan-to'g'ri state'ga bog'lamoqchi bo'lsak)
        // _uiState.value = _uiState.value?.copy(selectedCategoryIdsInternal = selectedIds)
    }


    fun saveRecipe() {
        // Saqlashdan OLDIN vaqtinchalik kategoriyalarni asosiy obyektga o'tkazish
        currentRecipe = currentRecipe.copy(categoryIds = tempSelectedCategoryIds.ifEmpty { null })
        Log.d(TAG, "saveRecipe called. Recipe to save categoryIds: ${currentRecipe.categoryIds}")

        // Validatsiya
        if (currentRecipe.name.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Retsept nomini kiriting")
            return
        }

        if (currentRecipe.categoryIds.isNullOrEmpty()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Kamida bitta kategoriya tanlang")
            return
        }
        if (selectedImageUri == null && currentRecipe.imageUrl.isNullOrEmpty()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Retsept rasmini tanlang")
            return
        }
        if (currentRecipe.ingredients.isNullOrEmpty()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Kamida bitta masalliq kiriting")
            return
        }
        if (currentRecipe.preparationSteps.isNullOrEmpty()) {
            _uiState.value =
                _uiState.value?.copy(errorMessage = "Kamida bitta tayyorlash qadamini kiriting")
            return
        }

        _uiState.value = _uiState.value?.copy(isSaving = true, errorMessage = null)
        Log.d(TAG, "saveRecipe: Saving process started...")

        viewModelScope.launch {
            val result: DataWrapper<*> = if (currentRecipe.id.isBlank()) {
                Log.d(TAG, "saveRecipe: Calling repository.addRecipe")
                recipeRepository.addRecipe(currentRecipe, selectedImageUri)
            } else {
                Log.d(TAG, "saveRecipe: Calling repository.updateRecipe")
                recipeRepository.updateRecipe(currentRecipe, selectedImageUri)
            }

            when (result) {
                is DataWrapper.Success<*> -> {
                    Log.d(TAG, "saveRecipe: Save successful.")
                    _uiState.postValue(_uiState.value?.copy(isSaving = false, saveSuccess = true))
                }

                is DataWrapper.Error<*> -> {
                    Log.e(TAG, "saveRecipe: Save failed: ${result.error}")
                    _uiState.postValue(
                        _uiState.value?.copy(isSaving = false, errorMessage = result.error)
                    )
                }

                is DataWrapper.Loading<*> -> {}
            }
        }
    }

    fun deleteRecipe() {
        if (currentRecipe.id.isBlank()) return
        Log.d(TAG, "deleteRecipe called for ID: ${currentRecipe.id}")
        _uiState.value = _uiState.value?.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = recipeRepository.deleteRecipe(currentRecipe)) {
                is DataWrapper.Success -> {
                    Log.d(TAG, "deleteRecipe: Delete successful.")
                    _uiState.postValue(_uiState.value?.copy(isSaving = false, deleteSuccess = true))
                }

                is DataWrapper.Error -> {
                    Log.e(TAG, "deleteRecipe: Delete failed: ${result.error}")
                    _uiState.postValue(
                        _uiState.value?.copy(
                            isSaving = false, errorMessage = "O'chirishda xatolik: ${result.error}"
                        )
                    )
                }

                is DataWrapper.Loading -> {}
            }
        }
    }

    fun hasChanges(): Boolean {
        val initialRecipe = try {
            if (initialRecipeJson != null) gson.fromJson(
                initialRecipeJson, Recipe::class.java
            ) else Recipe()
        } catch (e: Exception) {
            Recipe()
        }
        val currentRecipeForCheck =
            currentRecipe.copy(categoryIds = tempSelectedCategoryIds.ifEmpty { null }) // Tekshirish uchun joriy kategoriyalarni qo'shish
        val hasObjectChanged = currentRecipeForCheck != initialRecipe
        val hasImageChanged = selectedImageUri != null
        Log.d(
            TAG,
            "hasChanges: currentRecipe=$currentRecipeForCheck, initialRecipe=$initialRecipe, imageChanged=$hasImageChanged, objectChanged=$hasObjectChanged"
        )
        return hasObjectChanged || hasImageChanged
    }

    fun clearError() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }
}