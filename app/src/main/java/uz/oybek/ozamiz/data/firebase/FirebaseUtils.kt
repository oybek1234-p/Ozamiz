package uz.oybek.ozamiz.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

val currentUserId: String?
    get() = FirebaseAuth.getInstance().currentUser?.uid

val currentUserLogsRoot: DatabaseReference?
    get() = currentUserId?.let {
        FirebaseDatabase.getInstance().reference
            .child("logs")
            .child(it)
    }

val currentUserRoot: DatabaseReference?
    get() = currentUserId?.let {
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(it)
    }