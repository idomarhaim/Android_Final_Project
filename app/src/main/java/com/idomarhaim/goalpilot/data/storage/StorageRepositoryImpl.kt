package com.idomarhaim.goalpilot.data.storage

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.idomarhaim.goalpilot.core.result.Resource
import com.idomarhaim.goalpilot.core.util.IoDispatcher
import com.idomarhaim.goalpilot.domain.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    @IoDispatcher private val io: CoroutineDispatcher,
) : StorageRepository {

    override suspend fun uploadImage(folder: String, localUri: Uri): Resource<String> =
        withContext(io) {
            val uid = auth.currentUser?.uid ?: return@withContext Resource.Error("Not signed in")
            try {
                val ref = storage.reference.child("$folder/$uid/${UUID.randomUUID()}.jpg")
                ref.putFile(localUri).await()
                val url = ref.downloadUrl.await().toString()
                Resource.Success(url)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Image upload failed", e)
            }
        }

    override suspend fun deleteImage(downloadUrl: String): Resource<Unit> = withContext(io) {
        if (auth.currentUser == null) return@withContext Resource.Error("Not signed in")
        if (downloadUrl.isBlank()) return@withContext Resource.Success(Unit)
        try {
            storage.getReferenceFromUrl(downloadUrl).delete().await()
            Resource.Success(Unit)
        } catch (e: StorageException) {
            // "Already gone" is the outcome the caller asked for, so it is not an
            // error: a share deleted twice, or one whose image was cleaned up by a
            // half-finished earlier attempt, must not report a failure.
            if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                Resource.Success(Unit)
            } else {
                Resource.Error(e.message ?: "Could not delete image", e)
            }
        } catch (e: Exception) {
            // getReferenceFromUrl throws IllegalArgumentException on a URL that is
            // not a Firebase Storage one — a post could carry any string here.
            Resource.Error(e.message ?: "Could not delete image", e)
        }
    }
}
