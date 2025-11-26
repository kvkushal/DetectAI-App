package com.example.detectai

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class HistoryItem(
    @DocumentId
    var documentId: String? = null,
    var userId: String? = null,
    var type: String? = null,
    var result: String? = null,
    var originalData: String? = null,
    var detailedResult: String? = null,
    @ServerTimestamp
    var timestamp: Date? = null,
    var timestampLong: Long? = null
) : Parcelable