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

    /**
     * Deletes the object behind a download URL previously returned by
     * [uploadImage], so an image cannot outlive the thing that referenced it.
     *
     * A URL that resolves to nothing is **success**, not failure: the caller
     * wants the object gone, and it already is. Only the uploading user may
     * delete it — `storage.rules` scopes `write` (which covers delete) to the
     * uid segment in the path.
     */
    suspend fun deleteImage(downloadUrl: String): Resource<Unit>
}
