package uz.oybek.ozamiz.data.user.model.info

import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.appContext

enum class Cities(val resId: Int) {
    Hammasi(R.string.butun_uzbekiston),
    Toshkent(R.string.toshkent),
    Samarqand(R.string.samarqand),
    Nukus(R.string.nukus),
    Qashqadaryo(R.string.qashqa),
    Sirdaryo(R.string.sirdaryo),
    Surxondaryo(R.string.surxon),
    Termiz(R.string.termiz),
    Buxoro(R.string.buxoro),
    Andijon(R.string.andi),
    Navoiy(R.string.navoiy),
    Jizax(R.string.jizzah),
    Namangan(R.string.naman),
    Fargona(R.string.farg_ona),
    Qoqon(R.string.qo_qon),
    Xorazm(R.string.xorazm);

    companion object {
        fun asListNames(includeAll: Boolean = true) =
            entries.toMutableList().apply {
                if (includeAll.not()) {
                    remove(Hammasi)
                }
            }.map {
                appContext.getString(it.resId)
            }
    }
}