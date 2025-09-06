package uz.oybek.ozamiz.util

import java.util.UUID

object Id {

    fun newId() = UUID.randomUUID().toString()
}