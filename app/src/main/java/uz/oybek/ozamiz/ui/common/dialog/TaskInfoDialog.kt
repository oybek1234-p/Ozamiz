package uz.oybek.ozamiz.ui.common.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.points.model.Task

class TaskInfoDialog(context: Context, private val task: Task) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_task_info)
        
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

        findViewById<TextView>(R.id.tvTitle).text = task.title
        findViewById<TextView>(R.id.tvInfo).text = task.info
        
        findViewById<MaterialButton>(R.id.btnClose).setOnClickListener {
            dismiss()
        }
    }
}
