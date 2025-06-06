package uz.oybek.ozamiz.ui.log

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.gamification.GamificationConstants
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.databinding.FragmentFoodEnoughBinding
import uz.oybek.ozamiz.databinding.ItemRecommendedSnackBinding
import uz.oybek.ozamiz.ui.base.BaseAdapter
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.base.EmptyDiffUtil

class FoodEnoughFragment : BaseFragment<FragmentFoodEnoughBinding>() {

    private val args: FoodEnoughFragmentArgs by navArgs()
    private lateinit var snackAdapter: RecommendedSnackAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentFoodEnoughBinding {
        return FragmentFoodEnoughBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentFoodEnoughBinding) {
        val mealType = args.mealType
        val snacks = args.snacks.toList()
        val mealName = MealsType.fromId(mealType)?.name?.lowercase() ?: "ovqatlanish"

        val mealN = mealName.capitalize()
        bind.titleTextView.text = "$mealN ga energiya yetadi"
        bind.messageTextView.text =
            "${mealN} uchun yana ovqat yeyish tavsiya qilinmaydi, kaloriya miqdori oshsa semirishingiz mumkin"

        val nextMealType = when (mealType) {
            MealsType.Breakfast.id -> MealsType.Lunch.name.lowercase()
            MealsType.Lunch.id -> MealsType.Dinner.name.lowercase()
            MealsType.Dinner.id -> "Yotguncha"
            else -> null
        }

        val adviceText = when (nextMealType) {
            MealsType.Lunch.name.lowercase() ->
                "\uD83C\uDF4C Banan (yarim dona) – tabiiy energiya manbai\n" +
                        "\uD83E\uDD5B Kefir – ichakni sog‘lomlashtiradi\n" +
                        "\uD83C\uDF5E Qora non (1 tilim) – sekin hazm bo‘ladi, uzoq to‘q tutadi\n" +
                        "\uD83E\uDD5A Tuxum (qaynatilgan) – oqsil manbai, yengil tamaddi\n" +
                        "\uD83E\uDD55 Sabzi – tabiiy shirinlik, kaloriyasi kam\n" +
                        "\uD83C\uDF4E Olma – vitaminlar bilan boy, hazmga yengil\n" +
                        "\uD83E\uDED0 Rezavor mevalar – antioksidantlarga boy\n" +
                        "\uD83E\uDD63 Qatiq – probiotik, to‘yimli\n" +
                        "\uD83E\uDDC0 Tvorog – oqsilga boy, uzoq to‘q tutadi\n" +
                        "\uD83C\uDF4A Mandarin yoki apelsin – vitamin C manbai\n" +
                        "\uD83C\uDF4B Limonli choy (shakarsiz) – tetiklashtiradi, foydali\n" +
                        "\uD83C\uDF3E Ofsyanka suvi – to‘yimli, uzoq to‘q tutadi\n" +
                        "\uD83E\uDD5C Bodom (5–7 dona) – energiya beradi, to‘yimli\n" +
                        "\uD83C\uDF30 Yong‘oq (3–5 dona) – foydali yog‘lar manbai\n" +
                        "\uD83E\uDDFA Xurmo + yong‘oq – foydali va tabiiy tamaddi"

            MealsType.Dinner.name.lowercase() ->
                "\uD83E\uDD52 Bodring – suvga boy, hazmga yengil\n" +
                        "\uD83C\uDF4B Limonli choy (shakarsiz) – tetiklashtiradi, foydali\n" +
                        "\uD83C\uDF34 Xurmo (1 dona) – tabiiy shakar manbai\n" +
                        "\uD83C\uDF6B Qora shokolad (1 dona) – oz miqdorda, kayfiyatga yaxshi\n" +
                        "\uD83C\uDF3E Ofsyanka suvi – to‘yimli, uzoq to‘q tutadi\n" +
                        "\uD83E\uDDC0 Tvorog – oqsilga boy, uzoq to‘q tutadi\n" +
                        "\uD83E\uDD5C Bodom (5–7 dona) – energiya beradi, to‘yimli\n" +
                        "\uD83C\uDF30 Yong‘oq (3–5 dona) – foydali yog‘lar manbai\n" +
                        "\uD83C\uDF4E Olma – vitaminlar bilan boy, hazmga yengil\n" +
                        "\uD83E\uDD55 Sabzi – tabiiy shirinlik, kaloriyasi kam\n" +
                        "\uD83C\uDF4C Banan (yarim dona) – tabiiy energiya manbai\n" +
                        "\uD83E\uDD5B Kefir – ichakni sog‘lomlashtiradi\n" +
                        "\uD83E\uDD63 Qatiq – probiotik, to‘yimli\n" +
                        "\uD83E\uDD5A Tuxum (qaynatilgan) – oqsil manbai, yengil tamaddi\n" +
                        "\uD83C\uDF5E Qora non (1 tilim) – sekin hazm bo‘ladi, uzoq to‘q tutadi"

            "Yotguncha" ->
                "\uD83C\uDF75 Issiq yalpiz choyi (shakarsiz) – tinchlantiradi, uyquni yaxshilaydi\n" +
                        "\uD83C\uDF6F Asal (1 choy qoshiq) – tabiiy shirinlik, energiya beradi\n" +
                        "\uD83E\uDD5B Iliq sut (1 stakan) – tabiiy uyqu dori\n" +
                        "\uD83C\uDF4E Olma (mayda bo‘laklar) – yengil tamaddi, hazmga yordam\n" +
                        "\uD83E\uDD63 Suyuq jo‘xori (kashta) – hazmni yengillashtiradi\n" +
                        "\uD83C\uDF30 Yong‘oq (2-3 dona) – foydali yog‘lar, yengil tamaddi\n" +
                        "\uD83C\uDF4C Banan (yarim dona) – tabiiy magniy manbai, uyquga yordam\n" +
                        "\uD83E\uDD52 Bodring (bir nechta bo‘lak) – suvga boy, yengil tamaddi\n" +
                        "\uD83C\uDF6F Qatiq (kam yog‘li, 100 ml) – hazmga foydali, yengil\n" +
                        "\uD83C\uDF5E Qora non (1 tilim) – sekin hazm bo‘ladi, to‘q tutadi\n" +
                        "\uD83E\uDD5C Bodom (3-5 dona) – energiya beradi, yengil tamaddi\n" +
                        "\uD83E\uDED0 Rezavor mevalar (mayda porsiya) – antioksidantlar, yengil\n" +
                        "\uD83E\uDD5B Kefir (200 ml) – ichaklarni tinchlantiradi\n" +
                        "\uD83E\uDD55 Sabzi (mayda bo‘lak) – tabiiy shirinlik, kam kaloriyali\n" +
                        "\uD83E\uDDC0 Tvorog (oz miqdorda) – oqsil manbai, yengil"
            else -> ""
        }

// Keyin bu adviceText ni TextView ga o'rnatasiz:
// yourTextView.text = adviceText
        if (nextMealType != null) {
            bind.adviceMealTitle.text =
                "${nextMealType.replaceFirstChar { it.uppercase() }}gacha yesangiz bo'ladi (Tamaddi):"
            bind.adviceMealTitle.isVisible = true
            bind.adviceView.text = adviceText
        } else {
            bind.adviceMealTitle.isVisible = false
            bind.adviceView.isVisible = false
        }

        snackAdapter = RecommendedSnackAdapter()
        snackAdapter.submitList(snacks)

        bind.nextButton.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.foodEnoughFragment) {
                findNavController().popBackStack(R.id.home, false)
            }
        }
    }
}

class RecommendedSnackAdapter :
    BaseAdapter<GamificationConstants.RecommendedSnackFood, ItemRecommendedSnackBinding>(
        R.layout.item_recommended_snack, EmptyDiffUtil() // Agar itemlar o'zgarmasa
    ) {

    override fun inflate(
        inflater: LayoutInflater, parent: ViewGroup
    ): ItemRecommendedSnackBinding {
        return ItemRecommendedSnackBinding.inflate(inflater, parent, false)
    }

    override fun bind(
        holder: ViewHolder<*>, model: GamificationConstants.RecommendedSnackFood, pos: Int
    ) {
        val binding = holder.binding as ItemRecommendedSnackBinding
        binding.snackName.text = model.name
        binding.snackImage.setImageResource(model.iconRes)
        // Kerak bo'lsa tint qo'shish
        binding.snackImage.setColorFilter(
            ContextCompat.getColor(
                holder.itemView.context, R.color.iconColor
            )
        )
    }
}