package com.junkfood.seal.util

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import org.bouncycastle.openpgp.PGPObjectFactory
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider

/**
 * Verified yt-dlp runtime updater gate (design option b). Keeps the updater
 * feature but only installs a downloaded binary when every check passes:
 * pinned-release-key OpenPGP signature over SHA2-256SUMS, hash match for the
 * downloaded asset, monotonic version increase, size sanity, then an atomic
 * install with the fixed bundled baseline as the rollback target.
 *
 * The pinned key ships inside the app (raw resource). Rotation happens only
 * with a Seali release; a key mismatch fails closed (keep current binary).
 */
object YtDlpUpdateVerifier {

    private const val MIN_BYTES = 1L * 1024 * 1024
    private const val MAX_BYTES = 64L * 1024 * 1024

    class VerificationException(message: String) : Exception(message)

    /** yt-dlp versions are date-stamped: 2026.10.26[.N]. Strictly newer wins;
     *  tag equality and downgrades are rejected. */
    fun isNewerVersion(candidate: String, installed: String): Boolean {
        val c = candidate.split('.').map { it.toIntOrNull() ?: return false }
        val i = installed.split('.').map { it.toIntOrNull() ?: return false }
        require(c.size >= 3 && i.size >= 3) { "unexpected version format" }
        for (idx in 0 until maxOf(c.size, i.size)) {
            val cv = c.getOrElse(idx) { 0 }
            val iv = i.getOrElse(idx) { 0 }
            if (cv != iv) return cv > iv
        }
        return false
    }

    /** Verify a detached OpenPGP signature over [signedFile] using a public key
     *  whose fingerprint is in [pinnedFingerprints] (uppercase hex). Throws
     *  VerificationException on wrong key, bad/tampered signature, or missing
     *  signable content. */
    fun verifySignedSums(signedFile: File, signatureFile: File, pinnedFingerprints: Set<String>) =
        verifySignedSums(signedFile, signatureFile, resolvePublicKeyFile().readBytes(), pinnedFingerprints)

    /** Same verification with explicit key material (Android raw resource). */
    fun verifySignedSums(
        signedFile: File,
        signatureFile: File,
        publicKeyBytes: ByteArray,
        pinnedFingerprints: Set<String>,
    ) {
        val keyRings =
            PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(publicKeyBytes.inputStream()),
                JcaKeyFingerprintCalculator(),
            )
        val factory: PGPObjectFactory =
            JcaPGPObjectFactory(PGPUtil.getDecoderStream(FileInputStream(signatureFile)))
        var sigList: PGPSignatureList? = null
        while (true) {
            when (val obj = factory.nextObject()) {
                null -> break
                is PGPSignatureList -> { sigList = obj; break }
            }
        }
        if (sigList == null) throw VerificationException("no OpenPGP signature found")
        val signature = sigList.firstOrNull()
            ?: throw VerificationException("empty OpenPGP signature list")
        val key = keyRings.getPublicKey(signature.keyID)
            ?: throw VerificationException("signature made by an unknown key")
        val fingerprint = key.fingerprint.joinToString("") { "%02X".format(it) }
        if (fingerprint !in pinnedFingerprints) {
            throw VerificationException("signing key fingerprint $fingerprint is not pinned")
        }
        signature.init(JcaPGPContentVerifierBuilderProvider().setProvider("BC"), key)
        FileInputStream(signedFile).use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                signature.update(buffer, 0, read)
            }
        }
        if (!signature.verify()) {
            throw VerificationException("OpenPGP signature verification failed (tampered or corrupt sums file)")
        }
    }

    /** Expected SHA-256 of [assetName] from a verified SHA2-256SUMS file. */
    fun expectedHash(sumsFile: File, assetName: String): String {
        val line =
            sumsFile.readLines().firstOrNull { it.trimEnd().endsWith(" $assetName") }
                ?: throw VerificationException("no hash entry for $assetName in sums file")
        return line.split(Regex("\\s+")).first().lowercase()
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Full gate. Returns the verified file, ready for atomicInstall(). */
    fun verifyDownload(
        downloaded: File,
        assetName: String,
        candidateVersion: String,
        installedVersion: String,
        sumsFile: File,
        signatureFile: File,
        pinnedFingerprints: Set<String>,
        publicKeyBytes: ByteArray? = null,
    ): File {
        if (!isNewerVersion(candidateVersion, installedVersion)) {
            throw VerificationException("candidate $candidateVersion is not newer than installed $installedVersion")
        }
        val size = downloaded.length()
        if (size < MIN_BYTES || size > MAX_BYTES) {
            throw VerificationException("downloaded size $size outside sane bounds")
        }
        if (publicKeyBytes != null) {
            verifySignedSums(sumsFile, signatureFile, publicKeyBytes, pinnedFingerprints)
        } else {
            verifySignedSums(sumsFile, signatureFile, pinnedFingerprints)
        }
        val expected = expectedHash(sumsFile, assetName)
        val actual = sha256(downloaded)
        if (actual != expected) {
            throw VerificationException("SHA-256 mismatch (truncated or tampered download)")
        }
        return downloaded
    }

    /** Atomic install: same-directory temp + rename over the target. Callers
     *  keep the bundled baseline untouched, so any failure here leaves a
     *  non-vulnerable rollback state. */
    fun atomicInstall(verified: File, target: File) {
        val tmp = File(target.parentFile, target.name + ".install")
        verified.copyTo(tmp, overwrite = true)
        java.io.FileOutputStream(tmp, true).use { it.fd.sync() }
        if (!tmp.renameTo(target)) {
            tmp.delete()
            throw VerificationException("atomic rename failed; previous binary kept")
        }
    }

    /** The pinned release key ships as an app raw resource; this hook keeps
     *  the verifier JVM-testable (tests override it). */
    var publicKeyFileOverride: File? = null
    private fun resolvePublicKeyFile(): File =
        publicKeyFileOverride
            ?: throw VerificationException("no pinned public key available")
}
