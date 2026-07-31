package com.idomarhaim.goalpilot.domain.repository

import android.net.Uri
import com.idomarhaim.goalpilot.core.result.Resource

/** Image uploads to Firebase Storage (spec §4, §6 Core). */
interface StorageRepository {

    /**
     * Uploads the image at [localUri] into [folder] and returns its download URL.
     * The stored path is namespaced by the current user's uid.
     */
    suspend fun uploadImage(folder: String, localUri: Uri): Resource<String>
}
