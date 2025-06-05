package uz.oybek.ozamiz.ui.foods

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.foods.FoodAdvice
import uz.oybek.ozamiz.data.foods.FoodCategory
import uz.oybek.ozamiz.data.foods.SimpleMealIdea
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.data.upcomingMeal
import uz.oybek.ozamiz.databinding.FragmentFoodsBinding
import uz.oybek.ozamiz.databinding.ItemFoodAdviceBinding
import uz.oybek.ozamiz.databinding.ItemSimpleMealIdeaBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.makeExpandable
import uz.oybek.ozamiz.ui.showToast

class FoodsFragment : BaseFragment<FragmentFoodsBinding>() {

    private val viewModel: FoodsViewModel by viewModels()
    private lateinit var foodCategoryAdapter: FoodCategoryAdapter
    private lateinit var breakfastIdeasAdapter: SimpleMealIdeaAdapter
    private lateinit var lunchIdeasAdapter: SimpleMealIdeaAdapter
    private lateinit var dinnerIdeasAdapter: SimpleMealIdeaAdapter
    private lateinit var snackIdeasAdapter: SimpleMealIdeaAdapter

    private lateinit var popularRecipesAdapter: RecipeHorizontalAdapter

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFoodsBinding {
        return FragmentFoodsBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentFoodsBinding) {
        setupToolbarMenu()
        setupRecyclerViews(bind)
        setupClickListeners(bind)
        observeViewModel(bind)

        bind.swipeRefreshLayoutFoods.setOnRefreshListener {
            viewModel.loadInitialData(isRefresh = true)
        }

        parentFragmentManager.setFragmentResultListener(
            AddEditCategoryDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val success = bundle.getBoolean(AddEditCategoryDialog.RESULT_SUCCESS)
            if (success) {
                viewModel.loadInitialData(isRefresh = true)
            }
        }

        parentFragmentManager.setFragmentResultListener(
            "addEditSimpleMealRequestKey",
            viewLifecycleOwner
        ) { _, _ ->
            viewModel.loadInitialData(isRefresh = true) // Oddiy variant qo'shilsa/tahrirlansa qayta yuklash
        }
        parentFragmentManager.setFragmentResultListener(
            "addEditFoodAdviceRequestKey", // AddEditFoodAdviceDialog'dan keladigan unique key
            viewLifecycleOwner
        ) { _, _ ->
            // viewModel.loadInitialData(isRefresh = true) // Maslahat qo'shilsa/tahrirlansa qayta yuklash (hozircha o'chirilgan)
        }

    }

    private fun setupToolbarMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.recipe_feed_menu, menu) // recipe_feed_menu ishlatiladi

                val searchItem = menu.findItem(R.id.action_search_recipe) // ID ni to'g'rilash
                val searchView = searchItem?.actionView as? SearchView
                searchView?.queryHint = getString(R.string.search_food_hint)
                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        if (!query.isNullOrBlank()) {
                            viewModel.onSearchRequested(query)
                            searchView.clearFocus()
                            searchItem.collapseActionView()
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        return true
                    }
                })

                // Admin uchun item endi FoodsFragment ichida emas, umumiy menu orqali boshqariladi
                // yoki FAB orqali (agar shunday qaror qilingan bo'lsa)
                // val addAdminItem = menu.findItem(R.id.action_add_admin_item)
                // addAdminItem?.isVisible = viewModel.uiState.value.isUserAdmin
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    // R.id.action_add_admin_item -> { viewModel.onAdminAddClick(); true } // Bu qism o'chiriladi
                    else -> false
                }
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                // val addAdminItem = menu.findItem(R.id.action_add_admin_item)
                // addAdminItem?.isVisible = viewModel.uiState.value.isUserAdmin
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }


    private fun setupRecyclerViews(bind: FragmentFoodsBinding) {
        foodCategoryAdapter = FoodCategoryAdapter(
            onItemClick = { category -> viewModel.onCategoryClick(category) },
            onEditClick = { category ->
                if (viewModel.uiState.value.isUserAdmin) showAddEditCategoryDialog(
                    category
                )
            } // Admin tekshiruvi
        )
        bind.recyclerViewFoodCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = foodCategoryAdapter
            itemAnimator = null
        }

        breakfastIdeasAdapter =
            SimpleMealIdeaAdapter { mealIdea -> viewModel.onSimpleMealIdeaClick(mealIdea) }
        bind.recyclerViewBreakfastIdeas.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = breakfastIdeasAdapter
            itemAnimator = null
        }
        lunchIdeasAdapter =
            SimpleMealIdeaAdapter { mealIdea -> viewModel.onSimpleMealIdeaClick(mealIdea) }
        bind.recyclerViewLunchIdeas.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = lunchIdeasAdapter
            itemAnimator = null
        }
        dinnerIdeasAdapter =
            SimpleMealIdeaAdapter { mealIdea -> viewModel.onSimpleMealIdeaClick(mealIdea) }
        bind.recyclerViewDinnerIdeas.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dinnerIdeasAdapter
            itemAnimator = null
        }
        snackIdeasAdapter =
            SimpleMealIdeaAdapter { mealIdea -> viewModel.onSimpleMealIdeaClick(mealIdea) }
        bind.recyclerViewSnackIdeas.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = snackIdeasAdapter
            itemAnimator = null
        }
        popularRecipesAdapter = RecipeHorizontalAdapter(
            onItemClick = { recipe -> viewModel.onCategoryClick(LocalUser.user.upcomingMeal().let {
                val meal = MealsType.fromId(it)!!
                FoodCategory(meal.name.lowercase().trim(),meal.name)
            }) }
        )
        bind.recyclerViewPopularRecipes.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = popularRecipesAdapter
            itemAnimator = null
        }
    }

    private fun setupClickListeners(bind: FragmentFoodsBinding) {
        bind.buttonSeeAllRecipes.setOnClickListener {
            viewModel.onSeeAllRecipesClick()
        }
        bind.buttonSeeAllBreakfast.setOnClickListener {
            viewModel.onSeeAllSimpleMealsClick(
                MealsType.Breakfast.id,
                getString(R.string.breakfast) + " Variantlari"
            )
        }
        bind.buttonSeeAllLunch.setOnClickListener {
            viewModel.onSeeAllSimpleMealsClick(
                MealsType.Lunch.id,
                getString(R.string.lunch) + " Variantlari"
            )
        }
        bind.buttonSeeAllDinner.setOnClickListener {
            viewModel.onSeeAllSimpleMealsClick(
                MealsType.Dinner.id,
                getString(R.string.dinner) + " Variantlari"
            )
        }
        bind.buttonSeeAllSnack.setOnClickListener {
            viewModel.onSeeAllSimpleMealsClick(
                MealsType.Snack.id,
                getString(R.string.snack) + " Variantlari"
            )
        }

        bind.fabAddFoodItem.setOnClickListener { // FAB admin uchun
            if (viewModel.uiState.value.isUserAdmin) {
                showAdminAddOptionsDialog()
            }
        }

        bind.heavyFoodsButton.setOnClickListener {
            viewModel.onHeavyFoodsClick()
        }
    }

    private fun showAdminAddOptionsDialog() {
        val options = arrayOf(
            "Retsept Qo'shish",
            "Kategoriya Qo'shish",
            "Oddiy Variant Qo'shish"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Admin Amallari")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        if (findNavController().currentDestination?.id == R.id.foods) {
                            findNavController().navigate(R.id.action_foods_to_addEditRecipeFragment)
                        }
                    }

                    1 -> {
                        if (findNavController().currentDestination?.id == R.id.foods) {
                            showAddEditCategoryDialog(null)
                        }
                    }

                    2 -> {
                        if (findNavController().currentDestination?.id == R.id.foods) {
//                            val action = FoodsFragmentDirections.actionFoodsToAddEditSimpleMealIdeaDialog(null)
//                            findNavController().navigate(action)
                        }
                    }
                    /* Maslahatlar hozircha o'chirilgan
                    3 -> {
                        if (findNavController().currentDestination?.id == R.id.foods) {
                             val action = FoodsFragmentDirections.actionFoodsToAddEditFoodAdviceDialog(null)
                            findNavController().navigate(action)
                        }
                    }
                    */
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAddEditCategoryDialog(category: FoodCategory?) {
        if (findNavController().currentDestination?.id == R.id.foods) {
            val action =
                FoodsFragmentDirections.actionFoodsToAddEditCategoryDialog(categoryJson = category?.let {
                    com.google.gson.Gson().toJson(it)
                })
            findNavController().navigate(action)
        }
    }


    private fun observeViewModel(bind: FragmentFoodsBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    bind.progressBarFoodsOverall.isVisible = state.isOverallLoading
                    bind.swipeRefreshLayoutFoods.isRefreshing =
                        state.isOverallLoading && !state.categories.isEmpty()
                    bind.scrollViewContent.isVisible =
                        !state.isOverallLoading || !state.categories.isEmpty()

                    bind.recyclerViewFoodCategories.isVisible = !state.isLoadingCategories || state.categories.isNotEmpty()
                    bind.textViewCategoriesTitle.isVisible = !state.isLoadingCategories || state.categories.isNotEmpty()
                    if (!state.isLoadingCategories) foodCategoryAdapter.submitList(state.categories)

                    bind.textViewPopularRecipesTitle.text = MealsType.fromId(LocalUser.user.upcomingMeal())?.name + " retseptari"
                    bind.recyclerViewPopularRecipes.isVisible = !state.isLoadingPopularRecipes || state.popularRecipes.isNotEmpty()
                    bind.textViewPopularRecipesTitle.isVisible = !state.isLoadingPopularRecipes || state.popularRecipes.isNotEmpty()

                    bind.buttonSeeAllRecipes.isVisible =
                        !state.isLoadingPopularRecipes || state.popularRecipes.isNotEmpty()
                    if (!state.isLoadingPopularRecipes) popularRecipesAdapter.submitList(state.popularRecipes)


                    val showSimpleMealIdeas =
                        !state.isLoadingSimpleMealIdeas || state.simpleMealIdeasByMealType.isNotEmpty()

                      bind.buttonSeeAllBreakfast.isVisible = showSimpleMealIdeas && !state.simpleMealIdeasByMealType[MealsType.Breakfast.id].isNullOrEmpty()
                       breakfastIdeasAdapter.submitList(state.simpleMealIdeasByMealType[MealsType.Breakfast.id] ?: emptyList())
                       bind.recyclerViewBreakfastIdeas.isVisible = showSimpleMealIdeas && !state.simpleMealIdeasByMealType[MealsType.Breakfast.id].isNullOrEmpty()
                       bind.textViewBreakfastIdeasTitle.isVisible = showSimpleMealIdeas && !state.simpleMealIdeasByMealType[MealsType.Breakfast.id].isNullOrEmpty()

                       bind.buttonSeeAllLunch.isVisible = showSimpleMealIdeas && !state.simpleMealIdeasByMealType[MealsType.Lunch.id].isNullOrEmpty()
                       lunchIdeasAdapter.submitList(state.simpleMealIdeasByMealType[MealsType.Lunch.id] ?: emptyList())
                     bind.recyclerViewLunchIdeas.isVisible = showSimpleMealIdeas && !state.simpleMealIdeasByMealType[MealsType.Lunch.id].isNullOrEmpty()
                     bind.textViewLunchIdeasTitle.isVisible = showSimpleMealIdeas && !state.simpleMealIdeasByMealType[MealsType.Lunch.id].isNullOrEmpty()

                        bind.buttonSeeAllDinner.isVisible = showSimpleMealIdeas && !state.simpleMealIdeasByMealType[MealsType.Dinner.id].isNullOrEmpty()
                      dinnerIdeasAdapter.submitList(state.simpleMealIdeasByMealType[MealsType.Dinner.id] ?: emptyList())
                    bind.recyclerViewDinnerIdeas.isVisible = showSimpleMealIdeas && !state.simpleMealIdeasByMealType[MealsType.Dinner.id].isNullOrEmpty()
                       bind.textViewDinnerIdeasTitle.isVisible = showSimpleMealIdeas && !state.simpleMealIdeasByMealType[MealsType.Dinner.id].isNullOrEmpty()

                       bind.buttonSeeAllSnack.isVisible = showSimpleMealIdeas && !state.simpleMealIdeasByMealType[MealsType.Snack.id].isNullOrEmpty()
                       snackIdeasAdapter.submitList(state.simpleMealIdeasByMealType[MealsType.Snack.id] ?: emptyList())
                       bind.recyclerViewSnackIdeas.isVisible = showSimpleMealIdeas && !state.simpleMealIdeasByMealType[MealsType.Snack.id].isNullOrEmpty()
                      bind.textViewSnackIdeasTitle.isVisible = showSimpleMealIdeas && !state.simpleMealIdeasByMealType[MealsType.Snack.id].isNullOrEmpty()

                    bind.fabAddFoodItem.isVisible = state.isUserAdmin // Admin FAB ko'rinishi

                    state.errorMessage?.let { message ->
                        showToast(message)
                        viewModel.clearError()
                    }
                    requireActivity().invalidateOptionsMenu() // Menuni yangilash (admin item uchun)
                }
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { navEvent ->
                if (findNavController().currentDestination?.id != R.id.foods) return@let

                when (navEvent) {
                    is FoodsNavigationEvent.NavigateToFoodList -> {
                        val action = FoodsFragmentDirections.actionFoodsToFoodListFragment(
                            categoryId = navEvent.categoryId,
                            searchQuery = navEvent.searchQuery,
                            listTitle = navEvent.listTitle
                        )
                        findNavController().navigate(action)
                    }

                    is FoodsNavigationEvent.NavigateToRecipeDetail -> {
                        val action =
                            FoodsFragmentDirections.actionFoodsToRecipeDetailFragment(recipeJson = navEvent.recipeJson)
                        findNavController().navigate(action)
                    }

                    is FoodsNavigationEvent.NavigateToSimpleMealList -> {
                        val action =
                            FoodsFragmentDirections.actionFoodsFragmentToSimpleMealListFragment(
                                mealType = navEvent.mealType,
                                listTitle = navEvent.listTitle
                            )
                        findNavController().navigate(action)
                    }

                    is FoodsNavigationEvent.ShowSimpleMealDetail -> {
                        val bottomSheet = SimpleMealDetailBottomSheet.newInstance(navEvent.mealIdea)
                        bottomSheet.show(parentFragmentManager, SimpleMealDetailBottomSheet.TAG)
                    }

                    is FoodsNavigationEvent.NavigateToMonthlyMealPlan -> {

                    }

                    is FoodsNavigationEvent.NavigateToHeavyFoods -> {
                        navigate(R.id.heavyFoodsFragment)
                    }

                    is FoodsNavigationEvent.NavigateToFoodAdvice -> {

                    }

                    else -> {} // Boshqa eventlar uchun
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerViewFoodCategories.adapter = null
        binding.recyclerViewBreakfastIdeas.adapter = null
        binding.recyclerViewLunchIdeas.adapter = null
        binding.recyclerViewDinnerIdeas.adapter = null
        binding.recyclerViewSnackIdeas.adapter = null
        binding.recyclerViewPopularRecipes.adapter = null
        super.onDestroyView()
    }
}

class SimpleMealIdeaAdapter(
    private val onItemClick: (SimpleMealIdea) -> Unit
) : ListAdapter<SimpleMealIdea, SimpleMealIdeaAdapter.ViewHolder>(SimpleMealIdeaDiffCallback()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSimpleMealIdeaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSimpleMealIdeaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION)
                    onItemClick(getItem(bindingAdapterPosition))
            }
        }

        fun bind(idea: SimpleMealIdea) {
            binding.textViewIdeaName.text = idea.name
            binding.imageViewIdea.loadPhoto(idea.imageUrl ?: R.drawable.placeholder_food)
            binding.textViewIdeaDescription.text = idea.description
            if (idea.estimatedCalories != null && idea.estimatedCalories!! > 0) {
                binding.textViewIdeaCalories.text = itemView.context.getString(
                    R.string.calories_format_approx,
                    idea.estimatedCalories
                )
                binding.textViewIdeaCalories.visibility = android.view.View.VISIBLE
            } else {
                binding.textViewIdeaCalories.visibility = android.view.View.GONE
            }
        }
    }

    class SimpleMealIdeaDiffCallback : DiffUtil.ItemCallback<SimpleMealIdea>() {
        override fun areItemsTheSame(oldItem: SimpleMealIdea, newItem: SimpleMealIdea): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SimpleMealIdea, newItem: SimpleMealIdea): Boolean {
            return oldItem == newItem
        }
    }
}

class FoodAdviceAdapter :
    ListAdapter<FoodAdvice, FoodAdviceAdapter.ViewHolder>(FoodAdviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFoodAdviceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemFoodAdviceBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        init {
            binding.textViewAdviceContent.makeExpandable()
        }

        fun bind(advice: FoodAdvice) {
            binding.textViewAdviceTitle.text = advice.title
            binding.textViewAdviceTitle.isVisible = !advice.title.isNullOrBlank()

            binding.textViewAdviceContent.text = advice.content
            binding.textViewAdviceContent.maxLines = 4
        }
    }

    class FoodAdviceDiffCallback : DiffUtil.ItemCallback<FoodAdvice>() {
        override fun areItemsTheSame(oldItem: FoodAdvice, newItem: FoodAdvice): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FoodAdvice, newItem: FoodAdvice): Boolean {
            return oldItem == newItem
        }
    }
}