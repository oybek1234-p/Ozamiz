package uz.oybek.ozamiz.data.motivation

object MotivationRepository {

    fun getMessageForType(type: String): String {
        return NotificationMessages.getMessageForType(type)
    }
}
