package com.junkfood.seal.util

import java.io.File
import java.math.BigInteger
import java.security.Security
import java.util.Date
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPKeyPair
import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPPrivateKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureGenerator
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** JVM negative tests for the verified updater gate. */
class YtDlpUpdateVerifierTest {

    private lateinit var dir: File
    private lateinit var keyRing: PGPPublicKeyRing
    private lateinit var privateKey: PGPPrivateKey
    private lateinit var fingerprint: String

    @Before
    fun setUp() {
        Security.addProvider(BouncyCastleProvider())
        dir = createTempDir("verifier")
        val pair = JcaPGPKeyPair(PublicKeyAlgorithmTags.RSA_GENERAL, java.security.KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair(), java.util.Date())
        val gen =
            PGPKeyRingGenerator(
                PGPSignature.POSITIVE_CERTIFICATION,
                pair,
                "test <test@example.com>",
                null,
                PGPSignatureSubpacketGenerator().generate(),
                null,
                JcaPGPContentSignerBuilder(PublicKeyAlgorithmTags.RSA_GENERAL, HashAlgorithmTags.SHA256),
                null,
            )
        keyRing = gen.generatePublicKeyRing()
        privateKey = gen.generateSecretKeyRing().secretKey.extractPrivateKey(null)
        fingerprint = keyRing.publicKey.fingerprint.joinToString("") { "%02X".format(it) }
        File(dir, "public.key").outputStream().use { fos ->
            ArmoredOutputStream(fos).use { armor -> keyRing.encode(armor) }
        }
        YtDlpUpdateVerifier.publicKeyFileOverride = File(dir, "public.key")
    }

    @After
    fun tearDown() {
        YtDlpUpdateVerifier.publicKeyFileOverride = null
        dir.deleteRecursively()
    }

    private fun writeSums(hash: String = "0".repeat(64), asset: String = "yt-dlp"): File =
        File(dir, "SHA2-256SUMS").apply { writeText("$hash  $asset\n") }

    private fun sign(sums: File): File {
        val sigGen =
            PGPSignatureGenerator(
                JcaPGPContentSignerBuilder(keyRing.publicKey.algorithm, HashAlgorithmTags.SHA256)
            )
        sigGen.init(PGPSignature.BINARY_DOCUMENT, privateKey)
        sums.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                sigGen.update(buffer, 0, read)
            }
        }
        return File(dir, "SHA2-256SUMS.sig").apply {
            outputStream().use { fos ->
                ArmoredOutputStream(fos).use { armor -> sigGen.generate().encode(armor) }
            }
        }
    }

    private fun binary(bytes: Long = 2L * 1024 * 1024): File =
        File(dir, "yt-dlp").apply { writeBytes(ByteArray(bytes.toInt()) { 0x42 }) }

    @Test(expected = YtDlpUpdateVerifier.VerificationException::class)
    fun `tampered signature is rejected`() {
        val sums = writeSums(YtDlpUpdateVerifier.sha256(binary()))
        val sig = sign(sums)
        sums.appendText("tampered\n")
        YtDlpUpdateVerifier.verifySignedSums(sums, sig, setOf(fingerprint))
    }

    @Test(expected = YtDlpUpdateVerifier.VerificationException::class)
    fun `unpinned key is rejected`() {
        val sums = writeSums(YtDlpUpdateVerifier.sha256(binary()))
        val sig = sign(sums)
        YtDlpUpdateVerifier.verifySignedSums(sums, sig, setOf("A".repeat(40)))
    }

    @Test(expected = YtDlpUpdateVerifier.VerificationException::class)
    fun `missing hash entry is rejected`() {
        val sums = writeSums(asset = "other-asset")
        YtDlpUpdateVerifier.expectedHash(sums, "yt-dlp")
    }

    @Test(expected = YtDlpUpdateVerifier.VerificationException::class)
    fun `truncated download is rejected`() {
        val good = binary()
        val sums = writeSums(YtDlpUpdateVerifier.sha256(good))
        val sig = sign(sums)
        val truncated = File(dir, "yt-dlp-truncated").apply { writeBytes(good.readBytes().copyOf(1_500_000)) }
        YtDlpUpdateVerifier.verifyDownload(truncated, "yt-dlp", "2026.10.27", "2026.10.26", sums, sig, setOf(fingerprint))
    }

    @Test(expected = YtDlpUpdateVerifier.VerificationException::class)
    fun `downgrade is rejected`() {
        val good = binary()
        val sums = writeSums(YtDlpUpdateVerifier.sha256(good))
        val sig = sign(sums)
        YtDlpUpdateVerifier.verifyDownload(good, "yt-dlp", "2026.10.26", "2026.10.27", sums, sig, setOf(fingerprint))
    }

    @Test(expected = YtDlpUpdateVerifier.VerificationException::class)
    fun `equal tag is rejected`() {
        assertFalse(YtDlpUpdateVerifier.isNewerVersion("2026.10.26", "2026.10.26"))
        val good = binary()
        val sums = writeSums(YtDlpUpdateVerifier.sha256(good))
        val sig = sign(sums)
        YtDlpUpdateVerifier.verifyDownload(good, "yt-dlp", "2026.10.26", "2026.10.26", sums, sig, setOf(fingerprint))
    }

    @Test
    fun `genuine newer release verifies and installs atomically`() {
        val good = binary()
        val sums = writeSums(YtDlpUpdateVerifier.sha256(good))
        val sig = sign(sums)
        val verified =
            YtDlpUpdateVerifier.verifyDownload(good, "yt-dlp", "2026.10.27", "2026.10.26", sums, sig, setOf(fingerprint))
        val target = File(dir, "installed/yt-dlp").apply { parentFile.mkdirs(); writeText("old") }
        YtDlpUpdateVerifier.atomicInstall(verified, target)
        assertEquals(YtDlpUpdateVerifier.sha256(good), YtDlpUpdateVerifier.sha256(target))
    }

    @Test
    fun `version compare is monotonic across segments`() {
        assertTrue(YtDlpUpdateVerifier.isNewerVersion("2026.10.27", "2026.10.26"))
        assertTrue(YtDlpUpdateVerifier.isNewerVersion("2026.11.1", "2026.10.26"))
        assertTrue(YtDlpUpdateVerifier.isNewerVersion("2027.1.1", "2026.12.31"))
        assertFalse(YtDlpUpdateVerifier.isNewerVersion("2026.10.25", "2026.10.26"))
        assertFalse(YtDlpUpdateVerifier.isNewerVersion("2026.9.30", "2026.10.1"))
    }
}
