package uz.oybek.ozamiz.ui.common.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import androidx.core.graphics.drawable.toDrawable

class FullScreenImageDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val imageView = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val imageUrl = arguments?.getString("image_url")
        Glide.with(this).load(imageUrl).into(imageView)

        imageView.setOnClickListener { dismiss() }

        dialog.setContentView(imageView)
        dialog.window?.setBackgroundDrawable(Color.BLACK.toDrawable())
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        return dialog
    }

    fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
        show(fragmentManager, TAG)
    }

    companion object {
        const val TAG = "FullScreenImageDialog"
        private const val ARG_IMAGE_URL = "image_url"

        fun newInstance(imageUrl: String): FullScreenImageDialog {
            val args = Bundle().apply { putString(ARG_IMAGE_URL, imageUrl) }
            return FullScreenImageDialog().apply { arguments = args }
        }

        fun show(fragmentManager: androidx.fragment.app.FragmentManager, imageUrl: String): FullScreenImageDialog {
            val dialog = newInstance(imageUrl)
            dialog.show(fragmentManager)
            return dialog
        }
    }
}
