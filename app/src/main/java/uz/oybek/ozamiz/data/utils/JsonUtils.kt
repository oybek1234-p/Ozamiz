package uz.oybek.ozamiz.data.utils

import com.google.gson.Gson

var gson: Gson? = null
    get() {
        if (field == null) {
            field = Gson()
        }
        return field
    }

