package com.zeapo.pwdstore

import android.content.Context
import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.zeapo.pwdstore.crypto.PgpActivity
import kotlinx.android.synthetic.main.decrypt_layout.*
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


@RunWith(AndroidJUnit4::class)
@LargeTest
class DecryptTest {
    lateinit var targetContext: Context
    lateinit var testContext: Context
    lateinit var activity: PgpActivity

    val name = "sub"
    val parentPath = "/category/"
    lateinit var path: String
    lateinit var repoPath: String

    @Rule @JvmField
    var mActivityRule: ActivityTestRule<PgpActivity> = ActivityTestRule<PgpActivity>(PgpActivity::class.java, true, false)

    fun init() {
        targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        testContext = InstrumentationRegistry.getContext()
        copyAssets("encrypted-store", File(targetContext.filesDir, "test-store").absolutePath)
        repoPath = File(targetContext.filesDir, "test-store").absolutePath
        path = "$repoPath/$parentPath/$name.gpg".replace("//", "/")

        val intent = Intent(targetContext, PgpActivity::class.java)
        intent.putExtra("OPERATION", "DECRYPT")
        intent.putExtra("FILE_PATH", path)
        intent.putExtra("REPO_PATH", repoPath)

        activity = mActivityRule.launchActivity(intent)
    }

    @Test
    fun pathShouldDecompose() {
        init()

        assertEquals("/category/sub.gpg", PgpActivity.getRelativePath(path, repoPath))
        assertEquals("/category/", PgpActivity.getParentPath(path, repoPath))
        assertEquals("sub", PgpActivity.getName(path, repoPath))
        assertEquals("sub", PgpActivity.getName(path, "$repoPath/"))
    }

    @Test
    fun activityShouldShowName() {
        init()

        val categoryView = activity.crypto_password_category_decrypt
        assertNotNull(categoryView)
        assertEquals(parentPath, categoryView.text)

        val nameView = activity.crypto_password_file
        assertNotNull(nameView)
        assertEquals(name, nameView.text)
    }

    @Test
    fun shouldDecrypt() {
        init()

        activity.onBound(null)
        val clearPass = IOUtils.toString(
                IOUtils.toByteArray(testContext.assets.open("clear-store/category/sub")),
                Charsets.UTF_8.name()
        )
        val passEntry = PasswordEntry(clearPass)
        assertEquals(passEntry.password, activity.crypto_password_show.text)
        assertEquals(passEntry.username, activity.crypto_username_show.text.toString())
        assertEquals(passEntry.extraContent, activity.crypto_extra_show.text.toString())
    }

    companion object {
        fun copyAssets(source: String, destination: String) {
            FileUtils.forceMkdir(File(destination))
            FileUtils.cleanDirectory(File(destination))

            val testContext = InstrumentationRegistry.getContext()
            val assetManager = testContext.assets
            val files: Array<String>? = assetManager.list(source)

            files?.map { filename ->
                val destPath = "$destination/$filename"
                val sourcePath = "$source/$filename"

                if (assetManager.list(sourcePath).isNotEmpty()) {
                    FileUtils.forceMkdir(File(destination, filename))
                    copyAssets("$source/$filename", destPath)
                } else {
                    try {
                        val input = assetManager.open(sourcePath)
                        val outFile = File(destination, filename)
                        val output = FileOutputStream(outFile)
                        IOUtils.copy(input, output)
                        input.close()
                        output.flush()
                        output.close()
                    } catch (e: IOException) {
                        Log.e("tag", "Failed to copy asset file: " + filename, e)
                    }
                }
            }
        }
    }

}


