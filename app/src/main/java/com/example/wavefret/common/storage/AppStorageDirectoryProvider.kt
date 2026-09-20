package com.example.wavefret.common.storage

/**
 * Abstraction over the app's base storage location, so any feature that
 * needs to read or write files can depend on "where does this app store
 * its files" without depending on a concrete Android Context.
 */
interface AppStorageDirectoryProvider {
    /** @return Absolute path to the app's base storage directory. Type: String */
    fun baseDirectoryPath(): String
}