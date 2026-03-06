package com.example.photoorganizer.data.local.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

/**
 * Serializer for UserPreferences proto message.
 * Required by Proto DataStore for reading/writing protobuf data.
 *
 * Handles:
 * - Default values when no data exists
 * - Corruption detection and recovery
 * - Stream-based serialization for performance
 */
object UserPreferencesSerializer : Serializer<UserPreferences> {

    /**
     * Default value when no preferences file exists.
     * Used on first app launch.
     */
    override val defaultValue: UserPreferences = UserPreferences.getDefaultInstance()

    /**
     * Read UserPreferences from InputStream.
     * Throws CorruptionException if protobuf is invalid.
     */
    override suspend fun readFrom(input: InputStream): UserPreferences {
        try {
            return UserPreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto. Using default values.", exception)
        }
    }

    /**
     * Write UserPreferences to OutputStream.
     */
    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}
