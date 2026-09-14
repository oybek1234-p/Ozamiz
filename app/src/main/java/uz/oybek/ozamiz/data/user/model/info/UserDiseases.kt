package uz.oybek.ozamiz.data.user.model.info

import androidx.annotation.Keep
import uz.oybek.ozamiz.R

@Keep
enum class UserDiseases(val textRes: Int) {
    QANDLI_DIABET(R.string.qandli_diabet),
    EMIZIKLI(R.string.emizikli_ayol),
    HOMILADOR(R.string.homilador_ayol),
    YURAK_QON_TOMIR(R.string.infarkt)
}