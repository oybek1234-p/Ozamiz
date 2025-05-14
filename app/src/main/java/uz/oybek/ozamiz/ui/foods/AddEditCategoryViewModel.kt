package uz.oybek.ozamiz.ui.foods

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.foods.FoodCategory
import uz.oybek.ozamiz.data.foods.FoodCategoryRepository
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.showToast // Bu import mavjud emas edi, lekin kerak bo'lishi mumkin

class AddEditCategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val categoryRepository = FoodCategoryRepository()
    private val gson = Gson()

    private val _uiState = MutableLiveData(AddEditCategoryUiState()) // Bu State klassi ham kerak
    val uiState: LiveData<AddEditCategoryUiState> = _uiState

    private var currentCategory = FoodCategory()
    var selectedImageUri: Uri? = null // Fragmentdan o'rnatiladi

    private var initialCategoryJson: String? = null // O'zgarishlarni tekshirish uchun

    fun initialize(categoryJson: String?) {
        initialCategoryJson = categoryJson // Boshlang'ich holatni saqlash
        if (categoryJson != null) {
            try {
                currentCategory = gson.fromJson(categoryJson, FoodCategory::class.java)
                _uiState.value = _uiState.value?.copy(
                    categoryToEdit = currentCategory,
                    title = "Kategoriyani Tahrirlash"
                )
            } catch (e: Exception) {
                handleException(e, "CategoryJsonParseError")
                _uiState.value = _uiState.value?.copy(errorMessage = "Kategoriya ma'lumotlarini o'qishda xatolik")
            }
        } else {
            currentCategory = FoodCategory() // Yangi kategoriya
            _uiState.value = _uiState.value?.copy(
                categoryToEdit = null,
                title = "Yangi Kategoriya Qo'shish"
            )
        }
        selectedImageUri = null // Har safar ochilganda rasmni reset qilish
        // Boshlang'ich state ni tozalash
        _uiState.value = _uiState.value?.copy(saveSuccess = false, isSaving = false, errorMessage = null, deleteSuccess = false, canDelete = categoryJson != null)
    }

    fun saveCategory(name: String, orderStr: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Kategoriya nomini kiriting")
            return
        }
        val order = orderStr.toIntOrNull()
        if (order == null || order < 0) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Tartib raqamini to'g'ri kiriting (0 dan katta)")
            return
        }
        // Agar yangi kategoriya bo'lsa va rasm tanlanmagan bo'lsa (Ixtiyoriy: qat'iy talab qilsangiz)
        // if (currentCategory.id.isBlank() && selectedImageUri == null) {
        //     _uiState.value = _uiState.value?.copy(errorMessage = "Kategoriya rasmini tanlang")
        //     return
        // }

        _uiState.value = _uiState.value?.copy(isSaving = true, errorMessage = null)

        // Joriy obyektni yangilash
        // ID ni saqlab qolish muhim (agar tahrirlash bo'lsa)
        val categoryToSave = currentCategory.copy(name = name.trim(), order = order)

        viewModelScope.launch {
            val result = if (categoryToSave.id.isBlank()) {
                 // ID hali yo'q, demak yangi kategoriya
                 // Repository ID ni o'zi yaratishi kerak (yoki Firestore avtomatik)
                 // AddCategory hozir ID qaytarmaydi, String qaytaradigan qilib o'zgartirish kerak repositoryda
                categoryRepository.addCategory(categoryToSave, selectedImageUri) // Success<String> kutiladi
            } else {
                // Mavjud ID bor, demak tahrirlash
                categoryRepository.updateCategory(categoryToSave, selectedImageUri) // Success<Unit> kutiladi
            }

            when (result) {
                is DataWrapper.Success -> {
                    // Success tipi muhim emas, muvaffaqiyatli bo'ldi deb hisoblaymiz
                    _uiState.postValue(_uiState.value?.copy(isSaving = false, saveSuccess = true))
                }
                is DataWrapper.Error -> {
                    _uiState.postValue(_uiState.value?.copy(isSaving = false, errorMessage = result.error))
                }
                is DataWrapper.Loading -> {} // Saving state allaqachon o'rnatilgan
            }
        }
    }

     fun deleteCategory() {
         val categoryToDelete = _uiState.value?.categoryToEdit ?: return // Tahrirlanmayotgan bo'lsa chiqish
         if (categoryToDelete.id.isBlank()) return // ID bo'lmasa o'chirish mumkin emas

        _uiState.value = _uiState.value?.copy(isSaving = true, errorMessage = null) // Loading state
        viewModelScope.launch {
            when(val result = categoryRepository.deleteCategory(categoryToDelete.id)) {
                 is DataWrapper.Success -> {
                      _uiState.postValue(_uiState.value?.copy(isSaving = false, deleteSuccess = true)) // Success flag
                 }
                 is DataWrapper.Error -> {
                     _uiState.postValue(_uiState.value?.copy(isSaving = false, errorMessage = "O'chirishda xatolik: ${result.error}"))
                 }
                 is DataWrapper.Loading -> {}
            }
        }
     }

    fun hasChanges(name: String, orderStr: String): Boolean {
         val initial = try {
             if (initialCategoryJson != null) gson.fromJson(initialCategoryJson, FoodCategory::class.java) else FoodCategory()
         } catch (e: Exception) { FoodCategory() }

         val currentName = name.trim()
         val currentOrder = orderStr.toIntOrNull() ?: initial.order

         return currentName != initial.name || currentOrder != initial.order || selectedImageUri != null
    }

    fun clearError() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }

     fun resetSaveStatus() {
         // Dialog yopilgandan keyin yoki yangisi ochilganda statusni reset qilish uchun
         _uiState.value = _uiState.value?.copy(saveSuccess = false, deleteSuccess = false)
     }
}