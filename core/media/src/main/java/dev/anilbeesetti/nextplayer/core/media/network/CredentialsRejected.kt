package dev.anilbeesetti.nextplayer.core.media.network

import java.io.IOException

/**
 * The server refused the credentials the connection is saved with.
 *
 * Worth a type of its own because it is the one connection failure that retrying cannot fix: the
 * same credentials will be refused every time until someone enters them again.
 */
class CredentialsRejected(cause: Throwable) : IOException("The server rejected the saved credentials", cause)
