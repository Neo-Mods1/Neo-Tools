package com.neomods.tools.data

import com.neomods.tools.R
import com.neomods.tools.model.Tool

/**
 * Provides the tools contained in each [com.neomods.tools.model.Category].
 *
 * Every tool is a placeholder for now, but the catalogue is already structured
 * so each [Tool.id] can later be mapped to a concrete feature implementation.
 */
interface ToolRepository {

    /** All tools belonging to the category with [categoryId]. */
    fun getTools(categoryId: String): List<Tool>

    /** Lookup a single tool by [id] across all categories, or null. */
    fun getTool(id: String): Tool?

    /** Real-time search across every tool's title and description. */
    fun searchTools(query: String): List<Tool>
}

internal class DefaultToolRepository : ToolRepository {

    private val toolsByCategory: Map<String, List<Tool>> = mapOf(
        "apk" to listOf(
            Tool("apk_dex", "apk", "Dex Editor", "View and edit Dalvik bytecode", R.drawable.ic_tool_dex),
            Tool("apk_manifest", "apk", "Manifest Viewer", "Inspect the AndroidManifest", R.drawable.ic_tool_manifest),
            Tool("apk_manifest_edit", "apk", "Manifest Editor", "Edit manifest entries", R.drawable.ic_tool_manifest_edit),
            Tool("apk_smali", "apk", "Smali Viewer", "Browse disassembled smali", R.drawable.ic_tool_smali),
            Tool("apk_cert", "apk", "Certificate Viewer", "Inspect signing certificates", R.drawable.ic_tool_certificate),
            Tool("apk_info", "apk", "APK Info", "Package metadata and permissions", R.drawable.ic_tool_info),
            Tool("apk_resources", "apk", "Resource Browser", "Explore packaged resources", R.drawable.ic_tool_resources),
            Tool("apk_sign", "apk", "APK Signer", "Sign an APK with a key", R.drawable.ic_tool_sign),
            Tool("apk_zipalign", "apk", "ZipAlign", "Align an APK for performance", R.drawable.ic_tool_zip),
            Tool("apk_build", "apk", "Build APK", "Assemble a package from sources", R.drawable.ic_tool_build)
        ),
        "binary" to listOf(
            Tool("bin_hex", "binary", "Hex Viewer", "Hex dump of any file", R.drawable.ic_tool_hex),
            Tool("bin_diff", "binary", "File Diff", "Compare two files", R.drawable.ic_tool_diff),
            Tool("bin_checksum", "binary", "Checksum", "Compute MD5/SHA checksums", R.drawable.ic_tool_checksum)
        ),
        "image" to listOf(
            Tool("img_optimize", "image", "PNG Optimizer", "Compress PNG assets", R.drawable.ic_tool_image_edit),
            Tool("img_convert", "image", "Format Convert", "Convert between image formats", R.drawable.ic_tool_convert),
            Tool("img_exif", "image", "EXIF Viewer", "Read image metadata", R.drawable.ic_tool_exif)
        ),
        "xml" to listOf(
            Tool("xml_format", "xml", "XML Formatter", "Pretty-print XML", R.drawable.ic_tool_format),
            Tool("xml_validate", "xml", "XML Validator", "Validate against a schema", R.drawable.ic_tool_validate),
            Tool("xml_xpath", "xml", "XPath Tool", "Query XML with XPath", R.drawable.ic_tool_xpath)
        ),
        "encoding" to listOf(
            Tool("enc_base64", "encoding", "Base64", "Encode and decode Base64", R.drawable.ic_tool_base64),
            Tool("enc_url", "encoding", "URL Encode", "Percent-encode strings", R.drawable.ic_tool_url),
            Tool("enc_hex", "encoding", "Hex Encode", "Convert text to hex", R.drawable.ic_tool_hex)
        ),
        "text" to listOf(
            Tool("txt_case", "text", "Case Convert", "Switch text casing", R.drawable.ic_tool_case),
            Tool("txt_sort", "text", "Line Sort", "Sort lines alphabetically", R.drawable.ic_tool_sort),
            Tool("txt_regex", "text", "Regex", "Test regular expressions", R.drawable.ic_tool_regex)
        ),
        "crypto" to listOf(
            Tool("cry_hash", "crypto", "Hash", "MD5 / SHA / SHA-256 digests", R.drawable.ic_tool_hash),
            Tool("cry_aes", "crypto", "AES", "Symmetric encryption", R.drawable.ic_tool_default),
            Tool("cry_rsa", "crypto", "RSA", "Asymmetric key tools", R.drawable.ic_tool_rsa)
        ),
        "developer" to listOf(
            Tool("dev_http", "developer", "HTTP Client", "Send and inspect requests", R.drawable.ic_tool_http),
            Tool("dev_json", "developer", "JSON Viewer", "Format and query JSON", R.drawable.ic_tool_json),
            Tool("dev_sqlite", "developer", "SQLite", "Browse SQLite databases", R.drawable.ic_tool_sqlite)
        )
    )

    private val allTools = toolsByCategory.values.flatten()
    private val byId = allTools.associateBy { it.id }

    override fun getTools(categoryId: String): List<Tool> =
        toolsByCategory[categoryId].orEmpty()

    override fun getTool(id: String): Tool? = byId[id]

    override fun searchTools(query: String): List<Tool> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return allTools.filter {
            it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }
}
